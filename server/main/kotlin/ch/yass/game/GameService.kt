package ch.yass.game

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import ch.yass.core.contract.MDCAttributes.PARENT_TRACE_ID
import ch.yass.core.contract.MDCAttributes.TRACE_ID
import ch.yass.core.error.*
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
import ch.yass.identity.helper.isAnon
import kotlinx.coroutines.*
import org.slf4j.MDC
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.random.Random
import kotlinx.coroutines.channels.Channel as EventChannel

class GameService(
    private val repo: GameRepository,
    private val pubSub: PubSub,
    private val playerService: PlayerService,
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
    context(r: Raise<DomainError>)
    fun create(request: CreateCustomGameRequest, player: InternalPlayer): String {
        val settings = GameSettings.from(request)

        val validWcValue = when (settings.winningConditionType) {
            WinningConditionType.HANDS -> settings.winningConditionValue in 1..99
            WinningConditionType.POINTS -> settings.winningConditionValue in 100..9000
        }
        r.ensure(settings.botPositions().size < 4) { GameSettingsMaxBots(settings) }
        r.ensure(validWcValue) { GameSettingsInvalidValue(settings) }

        return startGame(settings, GameKind.CUSTOM, player).code
    }

    context(r: Raise<DomainError>)
    fun createDaily(player: InternalPlayer): String {
        r.ensure(!isAnon(player)) { SignedUpPlayersOnly(player) }

        val day = swissDay(LocalDateTime.now(ZoneOffset.UTC))
        val (start, end) = swissDayWindowUTC(day)

        // Already has a game of type DAILY today, meaning they already joined the daily challenge. We'll return
        // the code of that game which either results in an automatic rejoin if not finished or the analysis view.
        repo.getDailyGameForPlayer(player, start, end)?.let { return it.code }

        val challenge = repo.getOrCreateDailyChallengeForDay(day)
        val settings = GameSettings(
            botNorth = true, botEast = true, botSouth = false, botWest = true, // player will sit south
            winningConditionType = WinningConditionType.HANDS,
            winningConditionValue = 5,
            forcedDecks = challenge.forcedDecks
        )
        return startGame(settings, GameKind.DAILY, player, Position.SOUTH, challenge.seed).code
    }

    /**
     * Creates the game, seats all configured bots and the player (at [position] or a random free
     * one), then deals the first hand and starts its first trick.
     */
    context(_: Raise<GameAlreadyFull>)
    private fun startGame(
        settings: GameSettings,
        kind: GameKind,
        player: InternalPlayer,
        position: Position? = null,
        seed: Long = Random.nextLong(),
    ): Game {
        val game = repo.createGame(settings, kind, seed)

        settings.botPositions().forEach { botPosition ->
            repo.takeASeat(game, playerService.createBot(botPosition), botPosition)
        }
        val seat = repo.takeASeat(game, player, position)

        val hand = repo.createHand(NewHand(game, seat.position, dealHand(game, handNumber = 0)))
        repo.createTrick(hand)

        return game
    }

    context(r: Raise<GameError>, _: Raise<DbError>)
    fun join(request: JoinGameRequest, player: InternalPlayer): GameState {
        val game = repo.getByCode(request.code)
        r.ensure(!gameIsCanceled(game)) { GameAlreadyCanceled(game) }

        val joinedAtSeat = repo.takeASeat(game, player)
        val state = repo.getState(game)

        val actions = playerJoinedActions(state, player, joinedAtSeat)
        publishForSeats(state.seats) { actions }

        gameLoop(game)

        return repo.getState(game)
    }

    context(r: Raise<GameError>)
    fun cancel(request: CancelGameRequest, player: InternalPlayer): Game {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)

        r.ensure(playerInGame(player, state.seats)) { PlayerNotInGame(player, state) }
        r.ensure(gameIsCancelable(game)) { GameNotCancelable(game) }
        r.ensure(playerCreatedGame(player, state.seats)) { PlayerDidNotCreateGame(player, state) }

        val canceled = repo.cancelGame(game) ?: r.raise(GameNotCancelable(game))

        publishForSeats(state.seats) { seat ->
            if (seat.playerId == player.id) emptyList() else gameCanceledActions(game, player)
        }

        return canceled
    }

    /**
     * The best runs at today's daily challenge, ranked by the points the team of the player made. Only games
     * that were played to the end count, so the board stays empty until the first player of the day is through.
     */
    fun dailyLeaderboard(now: LocalDateTime): DailyLeaderboardResponse {
        val day = swissDay(now)
        val (start, end) = swissDayWindowUTC(day)

        val entries = repo.getFinishedDailyGames(start, end)
            .map { daily ->
                val points = pointsByPositionTotal(daily.hands, daily.tricks)
                val team = Team.entries.first { daily.position in it.positions }

                DailyLeaderboardEntry(daily.player, getTeamPoints(points, team))
            }
            .sortedByDescending { it.points }
            .take(15)

        return DailyLeaderboardResponse(day, entries)
    }

    /**
     * What the lobby needs to know about the player: the games they can rejoin and whether today's
     * daily challenge is already behind them.
     */
    fun info(player: InternalPlayer, now: LocalDateTime): GameInfoResponse {
        val (start, end) = swissDayWindowUTC(swissDay(now))
        val daily = repo.getDailyGameForPlayer(player, start, end)

        return GameInfoResponse(
            repo.getRunningGamesForPlayer(player).map(RunningGame::from),
            daily?.status == GameStatus.FINISHED
        )
    }

    context(_: Raise<GameWithCodeNotFound>)
    fun getStateByCode(code: String): GameState {
        val game = repo.getByCode(code)
        return repo.getState(game)
    }

    context(r: Raise<GameError>)
    fun play(request: PlayCardRequest, player: InternalPlayer): GameState {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)
        val playedCard = Card.from(request.card)
        val nextState = nextState(state)

        r.ensure(playerInGame(player, state.seats)) { PlayerNotInGame(player, state) }
        r.ensure(!gameIsCanceled(state.game)) { GameAlreadyCanceled(state.game) }
        r.ensure(expectedState(listOf(State.PLAY_CARD, State.PLAY_CARD_BOT), nextState)) {
            InvalidState(nextState, state)
        }
        r.ensure(playerHasActivePosition(player, state)) { PlayerIsLocked(player, state) }

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
        if (shouldWeisStoeck(updatedHand, weise, playerSeat.position, tricksOfHand(updatedState.tricks, updatedHand))) {
            weisStoeck(updatedState, playerSeat, updatedHand, weise)
        }

        gameLoop(game)

        return repo.getState(game)
    }

    context(r: Raise<GameError>)
    fun trump(request: ChooseTrumpRequest, player: InternalPlayer): GameState {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)
        val chosenTrump = Trump.valueOf(request.trump)
        val nextState = nextState(state)
        val position = state.seats.first { it.playerId == player.id }.position

        r.ensure(playerInGame(player, state.seats)) { PlayerNotInGame(player, state) }
        r.ensure(!gameIsCanceled(state.game)) { GameAlreadyCanceled(state.game) }
        r.ensure(expectedState(listOf(State.TRUMP, State.TRUMP_BOT), nextState)) { InvalidState(nextState, state) }
        r.ensure(playerHasActivePosition(player, state)) { PlayerIsLocked(player, state) }
        r.ensure(Trump.playable().contains(chosenTrump)) { TrumpInvalid(chosenTrump) }

        repo.chooseTrump(chosenTrump, currentHand(state.hands))

        val freshState = repo.getState(game)
        publishForSeats(state.seats) { seat -> trumpChosenActions(freshState, chosenTrump, position, seat) }

        gameLoop(game)

        return repo.getState(game)
    }

    context(r: Raise<GameError>)
    fun weisen(request: WeisenRequest, player: InternalPlayer): GameState {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)
        val nextState = nextState(state)
        val hand = currentHand(state.hands)
        val seat = playerSeat(player, state.seats)

        r.ensure(playerInGame(player, state.seats)) { PlayerNotInGame(player, state) }
        r.ensure(!gameIsCanceled(state.game)) { GameAlreadyCanceled(state.game) }
        r.ensure(expectedState(listOf(State.WEISEN_FIRST, State.WEISEN_FIRST_BOT), nextState)) {
            InvalidState(nextState, state)
        }
        r.ensure(playerHasActivePosition(player, state)) { PlayerIsLocked(player, state) }
        r.ensure(possibleWeise(hand.cardsOf(seat.position), hand.trump).contains(request.weis)) {
            WeisInvalid(request.weis)
        }

        val weise = hand.weiseOf(seat.position).toMutableList()
        weise.add(request.weis)

        repo.updateWeise(seat, hand, weise)

        val freshState = repo.getState(game)
        publishForSeats(state.seats) { gewiesenActions(freshState, request.weis, seat) }

        gameLoop(game)

        return repo.getState(game)
    }

    /**
     * After every player played the first card in the trick, the team who has gewiesen the most may
     * weis the rest of their potentially not yet declared weise. The server does this automatically
     * and store-only: nothing is published here. The reveal of all winning weise (declared and
     * auto-completed alike) happens via weisRevealActions once the trick completion is handled
     * in the game loop.
     */
    context(_: Raise<GameError>)
    private fun weisenSecond(state: GameState) {
        val hand = currentHand(state.hands)
        val remainingWeise = remainingWeise(hand)

        weisWinner(hand, state.tricks).forEach { position ->
            val weise = hand.weiseOf(position) + withoutStoeck(remainingWeise.getValue(position))
            repo.updateWeise(positionSeat(position, state.seats), hand, weise)
        }

        gameLoop(state.game)
    }

    context(r: Raise<GameError>)
    fun schiebe(request: SchiebeRequest, player: InternalPlayer): GameState {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)
        val nextState = nextState(state)
        val gschobe = Gschobe.valueOf(request.gschobe)
        val currentHand = currentHand(state.hands)
        val position = state.seats.first { it.playerId == player.id }.position

        r.ensure(playerInGame(player, state.seats)) { PlayerNotInGame(player, state) }
        r.ensure(!gameIsCanceled(state.game)) { GameAlreadyCanceled(state.game) }
        r.ensure(expectedState(listOf(State.SCHIEBE, State.SCHIEBE_BOT), nextState)) { InvalidState(nextState, state) }
        r.ensure(playerHasActivePosition(player, state)) { PlayerIsLocked(player, state) }

        repo.schiebe(gschobe, currentHand)

        val actions = geschobenActions(repo.getState(game), gschobe, position)
        publishForSeats(state.seats) { actions }

        gameLoop(game)

        return repo.getState(game)
    }

    context(_: Raise<SeatNotFound>)
    fun disconnectSeat(seatUUID: UUID) {
        val game = repo.getBySeatUUID(seatUUID.toString())
        val state = repo.getState(game)

        val dcSeat = state.seats.first { it.uuid == seatUUID }
        val dcPlayer = playerAtPosition(dcSeat.position, state.seats, state.allPlayers)
        val actions = playerDisconnectedActions(dcSeat, dcPlayer)

        repo.updateSeatStatus(dcSeat, SeatStatus.DISCONNECTED)
        publishForSeats(state.seats) { actions }
    }

    context(_: Raise<SeatNotFound>)
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
    context(_: Raise<GameError>)
    @OptIn(DelicateCoroutinesApi::class)
    private fun gameLoop(game: Game) {
        val updatedState = repo.getState(game)
        when (val nextStateLoop = nextState(updatedState)) {
            State.WAITING_FOR_PLAYERS -> {}
            State.FINISHED -> {
                repo.finishGame(game)
                val state = repo.getState(game)
                val actions = gameFinishedActions(state)
                logger().info("trigger_alert: Game finished ${game.code}")
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
                val revealActions = weisRevealActions(updatedState)
                repo.createTrick(currentHand(updatedState.hands))
                val state = repo.getState(game)
                publishForSeats(updatedState.seats) { seat -> revealActions + newTrickActions(state, seat) }
                gameLoop(game)
            }

            State.NEW_HAND -> {
                val startingPlayer = nextHandStartingPlayer(
                    updatedState.hands,
                    updatedState.allPlayers,
                    updatedState.seats
                )
                val startingPosition = playerSeat(startingPlayer, updatedState.seats).position
                val handNumber = updatedState.hands.size
                val newHand = repo.createHand(
                    NewHand(game, startingPosition, dealHand(updatedState.game, handNumber))
                )
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

    context(r: Raise<GameError>)
    private fun trumpAsBot(state: GameState): GameState {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)
        if (!botPlayer.bot) {
            r.raise(PlayerIsNotBot(botPlayer, state))
        }

        val candidate = chooseTrumpForBot(botPlayer, state)
        val request = ChooseTrumpRequest(state.game.uuid.toString(), candidate.trump.name)

        return trump(request, botPlayer)
    }

    context(r: Raise<GameError>)
    private fun playAsBot(state: GameState): GameState {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)
        if (!botPlayer.bot) {
            r.raise(PlayerIsNotBot(botPlayer, state))
        }

        val card = chooseCardForBot(botPlayer, state).card
        val request = PlayCardRequest(
            state.game.uuid.toString(),
            PlayedCard(card.suit.toString(), card.rank.toString())
        )

        return play(request, botPlayer)
    }

    context(r: Raise<GameError>)
    private fun schiebeAsBot(state: GameState): GameState {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)
        if (!botPlayer.bot) {
            r.raise(PlayerIsNotBot(botPlayer, state))
        }

        val gschobe = chooseGschobeForBot(botPlayer, state)
        val request = SchiebeRequest(state.game.uuid.toString(), gschobe.name)

        return schiebe(request, botPlayer)
    }


    context(_: Raise<GameError>)
    private fun weisenAsBot(state: GameState): GameState {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)

        val weis = chooseWeisForBot(botPlayer, state)
        val request = WeisenRequest(state.game.uuid.toString(), weis)

        return weisen(request, botPlayer)
    }

    /**
     * Deals the next hand: a forced deck (see GameSettings.forcedDecks, used to replicate
     * errors or for testing) always takes priority and is consumed once used, otherwise the
     * deal is derived deterministically from the game's seed and handNumber.
     */
    private fun dealHand(game: Game, handNumber: Int): EnumMap<Position, List<Card>> {
        val forcedDeck = game.settings.forcedDecks.firstOrNull()
        if (forcedDeck != null) {
            repo.updateSettings(game, game.settings.copy(forcedDecks = game.settings.forcedDecks.drop(1)))
        }
        return generateHand(seed = game.seed, handNumber = handNumber, forcedDeck = forcedDeck)
    }

}
