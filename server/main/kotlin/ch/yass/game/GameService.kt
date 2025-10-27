package ch.yass.game

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import ch.yass.admin.dsl.interpretCards
import ch.yass.core.contract.MDCAttributes.*
import ch.yass.core.error.*
import ch.yass.core.helper.associateWithToEnum
import ch.yass.core.helper.logger
import ch.yass.core.pubsub.Action
import ch.yass.core.pubsub.Channel
import ch.yass.core.pubsub.PubSub
import ch.yass.game.api.*
import ch.yass.game.api.internal.GameState
import ch.yass.game.api.internal.NewHand
import ch.yass.game.bot.chooseCardForBot
import ch.yass.game.bot.chooseGschobeForBot
import ch.yass.game.bot.chooseTrumpForBot
import ch.yass.game.bot.chooseWeisForBot
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.InternalPlayer
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.*
import ch.yass.game.pubsub.*
import kotlinx.coroutines.*
import org.slf4j.MDC
import java.util.*
import java.util.UUID
import kotlinx.coroutines.channels.Channel as EventChannel

class GameService(
    private val repo: GameRepository,
    private val pubSub: PubSub,
    private val playerService: PlayerService,
    private val foresight: Foresight,
) {
    /**
     * We use scope and eventChannel to also emmit our publishForSeats Actions. This helps greatly with
     * writing integration tests since we can wait for specific actions to complete in our coroutine contexts.
     */
    var scope = CoroutineScope(Dispatchers.Default + CoroutineExceptionHandler { _, exception ->
        logger().error("Exception in coroutine:", exception)
    })

    data class AsyncEvent(val seatUUID: UUID, val action: Action)

    /**
     * Gives us visibility on what the async bots are doing day in day out.
     */
    var eventChannel: EventChannel<AsyncEvent> = EventChannel()

    /**
     * This ensures that logging context (e.g., traceId) is available in async operations.
     */
    private fun launchWithMDC(block: suspend () -> Unit) {
        val mdcSnapshot = MDC.getCopyOfContextMap()
        scope.launch {
            if (mdcSnapshot != null) {
                MDC.put(TRACE_ID.value, UUID.randomUUID().toString())
                MDC.put(PARENT_TRACE_ID.value, mdcSnapshot[TRACE_ID.value])
            }

            block()
        }
    }

    /**
     * Validate settings, create a new game in the db, create players for all bots and seat them
     * as configured in the settings. Then seat the player at a free position, deal everyone
     * some cards and start a fresh trick.
     */
    context(Raise<DomainError>)
    fun create(request: CreateCustomGameRequest, player: InternalPlayer): String {
        val settings = GameSettings.from(request)

        val validWcValue = when (settings.winningConditionType) {
            WinningConditionType.HANDS -> settings.winningConditionValue in 1..99
            WinningConditionType.POINTS -> settings.winningConditionValue in 100..9000
        }
        ensure(settings.botPositions().size < 4) { GameSettingsMaxBots(settings) }
        ensure(validWcValue) { GameSettingsInvalidValue(settings) }

        val game = repo.createGame(settings)

        settings.botPositions().map { position ->
            val botPlayer = playerService.create("Bot", position)
            repo.takeASeat(game, botPlayer, position)
        }
        val newSeat = repo.takeASeat(game, player)

        // TODO: This will need some refactoring: The welcome hand should be related to what cards the player
        //       unlocked so we can't deal the full hand yet if we don't know all the players. This should
        //       be done when the player joins at the table.
        // Creating player always starts game
        val cards = Position.entries.associateWithToEnum { interpretCards("welcome") }
        val hand = repo.createHand(NewHand(game, newSeat.position, cards, Trump.FREESTYLE, Gschobe.NO))
        repo.createTrick(hand)

        return game.code
    }

    context(Raise<GameError>, Raise<DbError>)
    fun join(request: JoinGameRequest, player: InternalPlayer): GameState {
        val game = repo.getByCode(request.code)
        val joinedAtSeat = repo.takeASeat(game, player)
        val state = repo.getState(game)

        val actions = playerJoinedActions(state, player, joinedAtSeat)
        publishForSeats(state.seats) { actions }

        gameLoop(game)

        return repo.getState(game)
    }

    context(Raise<GameWithCodeNotFound>)
    fun getStateByCode(code: String): GameState {
        val game = repo.getByCode(code)
        return repo.getState(game)
    }

    context(Raise<GameError>)
    fun play(request: PlayCardRequest, player: InternalPlayer): GameState {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)
        val playedCard = Card.from(request.card)
        val nextState = nextState(state)

        ensure(playerInGame(player, state.seats)) { PlayerNotInGame(player, state) }
        ensure(expectedState(listOf(State.PLAY_CARD, State.PLAY_CARD_BOT), nextState)) {
            InvalidState(nextState, state)
        }
        ensure(playerHasActivePosition(player, state)) { PlayerIsLocked(player, state) }

        cardIsPlayable(playedCard, player, state)

        val currentTrick = currentTrick(state.tricks)
        val playerSeat = playerSeat(player, state.seats)

        repo.playCard(playedCard, currentTrick, playerSeat)
        val updatedState = repo.getState(game)

        publishForSeats(state.seats) { seat -> cardPlayedActions(updatedState, playedCard, playerSeat, seat) }

        // TODO: Move this logic to game state but watch out: playing a card means it's the next players active
        //      turn per activePosition logic so the stoeck can't be played anymore. We need to update
        //      the activePosition logic for that
        val updatedHand = currentHand(updatedState.hands)
        val weise = possibleWeise(updatedHand.cardsOf(playerSeat.position), updatedHand.trump)
        if (!isStoeckGewiesen(updatedHand, weise, playerSeat.position, updatedState.tricks)) {
            weisStoeck(updatedState, playerSeat, updatedHand, weise)
        }

        gameLoop(game)

        return repo.getState(game)
    }

    context(Raise<GameError>)
    fun trump(request: ChooseTrumpRequest, player: InternalPlayer): GameState {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)
        val chosenTrump = Trump.valueOf(request.trump)
        val nextState = nextState(state)
        val position = state.seats.first { it.playerId == player.id }.position

        ensure(playerInGame(player, state.seats)) { PlayerNotInGame(player, state) }
        ensure(expectedState(listOf(State.TRUMP, State.TRUMP_BOT), nextState)) { InvalidState(nextState, state) }
        ensure(playerHasActivePosition(player, state)) { PlayerIsLocked(player, state) }
        ensure(Trump.playable().contains(chosenTrump)) { TrumpInvalid(chosenTrump) }

        repo.chooseTrump(chosenTrump, currentHand(state.hands))

        val freshState = repo.getState(game)
        publishForSeats(state.seats) { seat -> trumpChosenActions(freshState, chosenTrump, position, seat) }

        gameLoop(game)

        return repo.getState(game)
    }

    context(Raise<GameError>)
    fun weisen(request: WeisenRequest, player: InternalPlayer): GameState {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)
        val nextState = nextState(state)
        val hand = currentHand(state.hands)
        val seat = playerSeat(player, state.seats)

        ensure(playerInGame(player, state.seats)) { PlayerNotInGame(player, state) }
        ensure(expectedState(listOf(State.WEISEN_FIRST, State.WEISEN_FIRST_BOT), nextState)) {
            InvalidState(nextState, state)
        }
        ensure(playerHasActivePosition(player, state)) { PlayerIsLocked(player, state) }
        ensure(possibleWeise(hand.cardsOf(seat.position), hand.trump).contains(request.weis)) {
            WeisInvalid(request.weis)
        }

        val weise = hand.weiseOf(seat.position).toMutableList()
        weise.add(request.weis)

        repo.updateWeise(seat, hand, weise)

        val freshState = repo.getState(game)
        publishForSeats(state.seats) { gewiesenActions(freshState, request.weis, seat, it) }

        gameLoop(game)

        return repo.getState(game)
    }

    /**
     * After every player played the first card in the trick and showed their weise, the team who has wisen
     * the most can weis the rest of their potentially not yet shown weise. This can lead to behaviour where
     * a player sees their own - not made - weis. E.g. a player has two possible weise and they show the first
     * one, then after the first trick is over this method automatically publishes ShowWeis actions for all
     * valid but not shown weise of the team with the most weis points. A bit a strange rule...
     */
    context(Raise<GameError>)
    private fun weisenSecond(state: GameState) {
        val hand = currentHand(state.hands)
        val remainingWeise = remainingWeise(hand)

        weisWinner(hand, state.tricks).map { position ->
            val weise = hand.weiseOf(position).toMutableList()
            val actions = withoutStoeck(remainingWeise.getValue(position))
                .map {
                    weise.add(it)
                    ShowWeis(position, it.toWeisWithPoints(hand.trump))
                }

            repo.updateWeise(positionSeat(position, state.seats), hand, weise)
            publishForSeats(state.seats) { actions }
        }

        val points = pointsByPositionTotal(completedHands(state.hands, state.tricks), state.tricks)
        publishForSeats(state.seats) { listOf(UpdatePoints(points)) }

        gameLoop(state.game)
    }

    context(Raise<GameError>)
    fun schiebe(request: SchiebeRequest, player: InternalPlayer): GameState {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)
        val nextState = nextState(state)
        val gschobe = Gschobe.valueOf(request.gschobe)
        val currentHand = currentHand(state.hands)
        val position = state.seats.first { it.playerId == player.id }.position

        ensure(playerInGame(player, state.seats)) { PlayerNotInGame(player, state) }
        ensure(expectedState(listOf(State.SCHIEBE, State.SCHIEBE_BOT), nextState)) { InvalidState(nextState, state) }
        ensure(playerHasActivePosition(player, state)) { PlayerIsLocked(player, state) }

        repo.schiebe(gschobe, currentHand)

        val actions = geschobenActions(repo.getState(game), gschobe, position)
        publishForSeats(state.seats) { actions }

        gameLoop(game)

        return repo.getState(game)
    }

    context(Raise<SeatNotFound>)
    fun disconnectSeat(seatUUID: UUID) {
        val game = repo.getBySeatUUID(seatUUID.toString())
        val state = repo.getState(game)

        val dcSeat = state.seats.first { it.uuid == seatUUID }
        val dcPlayer = playerAtPosition(dcSeat.position, state.seats, state.allPlayers)
        val actions = playerDisconnectedActions(dcSeat, dcPlayer)

        repo.updateSeatStatus(dcSeat, SeatStatus.DISCONNECTED)
        publishForSeats(state.seats) { actions }
    }

    context(Raise<SeatNotFound>)
    fun connectSeat(seat: Seat) {
        val game = repo.getBySeatUUID(seat.uuid.toString())
        val state = repo.getState(game)
        val player = playerAtPosition(seat.position, state.seats, state.allPlayers)
        val actions = playerJoinedActions(state, player, seat)

        repo.updateSeatStatus(seat, SeatStatus.CONNECTED)
        publishForSeats(state.seats) { actions }
    }

    private fun publishForSeats(seats: List<Seat>, action: (Seat) -> List<Action>) {
        launchWithMDC {
            seats.forEach { seat ->
                action.invoke(seat)
                    .forEach { action -> eventChannel.send(AsyncEvent(seat.uuid, action)) }
            }
        }

        // Don't send ws events for bots or disconnected clients
        seats.filter { it.status !in listOf(SeatStatus.BOT, SeatStatus.DISCONNECTED) }
            .forEach { pubSub.publish(action.invoke(it), Channel("seat", it.uuid)) }
    }

    /**
     * Controlling our game state. There are some special cases where the game engine is responsible
     * for the next action and not the user:
     *
     * - PLAY_CARD_BOT -> Wait, then play the card async
     * - NEW_TRICK -> Wait 1s before creating the new trick async
     * - NEW_TRICK -> Call gameLoop again, the next state could e.g. be PLAY_CARD_BOT
     * - NEW_HAND -> Call gameLoop again, the next state again could be a BOT action
     *
     * TODO: Add delays on client side
     */
    context(Raise<GameError>)
    @OptIn(DelicateCoroutinesApi::class)
    private fun gameLoop(game: Game) {
        val updatedState = repo.getState(game)
        when (val nextStateLoop = nextState(updatedState)) {
            State.WAITING_FOR_PLAYERS -> {}
            State.FINISHED -> {
                repo.finishGame(game)
                val state = repo.getState(game)
                val actions = gameFinishedActions(state)
                publishForSeats(updatedState.seats) { actions }
            }

            State.PLAY_CARD -> {
                val activePosition = activePosition(updatedState.hands, updatedState.seats, updatedState.tricks)
                val actions = listOf(
                    UpdateActive(activePosition),
                    UpdateState(nextStateLoop),
                )
                publishForSeats(updatedState.seats) { actions }
            }

            State.TRUMP -> {}
            State.SCHIEBE -> {}
            State.PLAY_CARD_BOT -> launchWithMDC { playAsBot(updatedState) }
            State.TRUMP_BOT -> launchWithMDC { trumpAsBot(updatedState) }
            State.SCHIEBE_BOT -> launchWithMDC { schiebeAsBot(updatedState) }
            State.WEISEN_FIRST -> {}
            State.WEISEN_FIRST_BOT -> launchWithMDC { weisenAsBot(updatedState) }
            State.WEISEN_SECOND -> weisenSecond(updatedState)

            State.WEISEN_SECOND_BOT -> launchWithMDC { weisenAsBot(updatedState) }
            State.NEW_TRICK -> {
                repo.createTrick(currentHand(updatedState.hands))
                val state = repo.getState(game)
                publishForSeats(updatedState.seats) { seat -> newTrickActions(state, seat) }
                gameLoop(game)
            }

            State.NEW_HAND -> {
                val startingPlayer = nextHandStartingPlayer(
                    updatedState.hands,
                    updatedState.allPlayers,
                    updatedState.seats
                )
                val startingPosition = playerSeat(startingPlayer, updatedState.seats).position
                val newHand = repo.createHand(NewHand(game, startingPosition, randomHand(foresight.nextDeck())))
                repo.createTrick(newHand)
                val state = repo.getState(game)
                publishForSeats(updatedState.seats) { seat -> newHandActions(state, seat) }
                gameLoop(game)
            }
        }
    }

    /**
     * Not sure if it makes sense to show the user a UI where he has to actually apply the stoeck so currently
     * this is all done automatically within the card play request. This means that this function is pretty implicit
     * without using a "request". It assumes the state is already correct.
     */
    private fun weisStoeck(state: GameState, seat: Seat, hand: Hand, weise: List<Weis>) {
        val stoeck = weise.first { w -> w.type == WeisType.STOECK }
        val currentWeise = hand.weiseOf(seat.position).toMutableList()
        currentWeise.add(stoeck)

        repo.updateWeise(seat, hand, currentWeise)

        val actions = stoeckGewiesenActions(hand, stoeck, seat, state)
        publishForSeats(state.seats) { actions }
    }

    context(Raise<GameError>)
    private fun trumpAsBot(state: GameState): GameState {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)
        if (!botPlayer.bot) {
            raise(PlayerIsNotBot(botPlayer, state))
        }

        val candidate = chooseTrumpForBot(botPlayer, state)
        val request = ChooseTrumpRequest(state.game.uuid.toString(), candidate!!.trump.name)

        return trump(request, botPlayer)
    }

    context(Raise<GameError>)
    private fun playAsBot(state: GameState): GameState {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)
        if (!botPlayer.bot) {
            raise(PlayerIsNotBot(botPlayer, state))
        }

        val card = chooseCardForBot(botPlayer, state).card
        val request = PlayCardRequest(
            state.game.uuid.toString(),
            PlayedCard(card.suit.toString(), card.rank.toString(), card.skin)
        )

        return play(request, botPlayer)
    }

    context(Raise<GameError>)
    private fun schiebeAsBot(state: GameState): GameState {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)
        if (!botPlayer.bot) {
            raise(PlayerIsNotBot(botPlayer, state))
        }

        val gschobe = chooseGschobeForBot(botPlayer, state)
        val request = SchiebeRequest(state.game.uuid.toString(), gschobe.name)

        return schiebe(request, botPlayer)
    }


    context(Raise<GameError>)
    private fun weisenAsBot(state: GameState): GameState {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)

        // TODO: Better decision
        val weis = chooseWeisForBot(botPlayer, state)
        val request = WeisenRequest(state.game.uuid.toString(), weis)

        return weisen(request, botPlayer)
    }

}