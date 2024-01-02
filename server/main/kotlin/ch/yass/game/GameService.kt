package ch.yass.game

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import ch.yass.admin.dsl.interpretCards
import ch.yass.core.error.*
import ch.yass.core.pubsub.Action
import ch.yass.core.pubsub.Channel
import ch.yass.core.pubsub.PubSub
import ch.yass.game.api.*
import ch.yass.game.api.internal.GameState
import ch.yass.game.api.internal.NewHand
import ch.yass.game.api.internal.NewPlayer
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.*
import ch.yass.game.pubsub.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class GameService(
    private val repo: GameRepository,
    private val pubSub: PubSub,
    private val playerService: PlayerService
) {

    /**
     * Validate settings, create a new game in the db, create players for all bots and seat them
     * as configured in the settings. Then seat the player at a free position, deal everyone
     * some cards and start a fresh trick.
     */
    context(Raise<GameError>)
    fun create(request: CreateCustomGameRequest, player: Player): String {
        val settings = GameSettings.from(request)

        val validWcValue = when (settings.winningConditionType) {
            WinningConditionType.HANDS -> settings.winningConditionValue in 1..99
            WinningConditionType.POINTS -> settings.winningConditionValue in 100..9000
        }
        ensure(settings.botPositions().size < 4) { GameSettingsMaxBots(settings) }
        ensure(validWcValue) { GameSettingsInvalidValue(settings) }

        val game = repo.createGame(settings)
        val botNames = arrayOf(
            "Rolf", "Heidi", "Urs", "Elsbeth", "Matthias", "François", "Chantal", "Pierre", "Brigitte", "Michel",
            "Giuseppe", "Maria", "Marco", "Lucia", "Roberto", "Gian", "Anna", "Silvan", "Petra", "Lukas"
        )

        settings.botPositions().map { position ->
            val botPlayer = playerService.create(NewPlayer(UUID.randomUUID(), botNames.random(), true))
            repo.takeASeat(game, botPlayer, position)
        }
        repo.takeASeat(game, player)

        // TODO: This will need some refactoring: The welcome hand should be related to what cards the player
        //       unlocked so we can't deal the full hand yet if we don't know all the players. This should
        //       be done when the player joins at the table.
        // Creating player always starts game
        val cards = Position.entries.associateWith { interpretCards("welcome") }
        val hand = repo.createHand(NewHand(game, player, cards, Trump.FREESTYLE, Gschobe.NO))
        repo.createTrick(hand)

        return game.code
    }

    context(Raise<GameError>, Raise<DbError>)
    fun join(request: JoinGameRequest, player: Player): GameState {
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
    fun play(request: PlayCardRequest, player: Player): GameState {
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

        val currentTrick = currentTrick(state.tricks)!!
        val playerSeat = playerSeat(player, state.seats)

        repo.playCard(playedCard, currentTrick, playerSeat)
        val updatedState = repo.getState(game)

        publishForSeats(state.seats) { seat -> cardPlayedActions(updatedState, playedCard, playerSeat, seat) }

        gameLoop(game)

        return updatedState
    }


    context(Raise<GameError>)
    fun trump(request: ChooseTrumpRequest, player: Player): GameState {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)
        val chosenTrump = Trump.valueOf(request.trump)
        val nextState = nextState(state)
        val currentHand = currentHand(state.hands)!!

        ensure(playerInGame(player, state.seats)) { PlayerNotInGame(player, state) }
        ensure(expectedState(listOf(State.TRUMP, State.TRUMP_BOT), nextState)) { InvalidState(nextState, state) }
        ensure(playerHasActivePosition(player, state)) { PlayerIsLocked(player, state) }
        ensure(playableTrumps().contains(chosenTrump)) { TrumpInvalid(chosenTrump) }

        repo.chooseTrump(chosenTrump, currentHand)

        val actions = trumpChosenActions(repo.getState(game), chosenTrump)
        publishForSeats(state.seats) { actions }

        gameLoop(game)

        return repo.getState(game)
    }

    context(Raise<GameError>)
    fun schiebe(request: SchiebeRequest, player: Player): GameState {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)
        val nextState = nextState(state)
        val gschobe = Gschobe.valueOf(request.gschobe)
        val currentHand = currentHand(state.hands)!!

        ensure(playerInGame(player, state.seats)) { PlayerNotInGame(player, state) }
        ensure(expectedState(listOf(State.SCHIEBE, State.SCHIEBE_BOT), nextState)) { InvalidState(nextState, state) }
        ensure(playerHasActivePosition(player, state)) { PlayerIsLocked(player, state) }

        repo.schiebe(gschobe, currentHand)

        val actions = schiebeActions(repo.getState(game))
        publishForSeats(state.seats) { actions }

        gameLoop(game)

        return repo.getState(game)
    }

    context(Raise<SeatNotFound>)
    fun disconnectSeat(seatUUID: UUID) {
        val game = repo.getBySeatUUID(seatUUID.toString())
        val state = repo.getState(game)

        val dcSeat = state.seats.first { it.uuid == seatUUID }
        val dcPlayer = playerAtPosition(dcSeat.position, state.seats, state.allPlayers)!!
        val actions = playerDisconnectedActions(state, dcSeat, dcPlayer)

        repo.updateSeatStatus(dcSeat, SeatStatus.DISCONNECTED)
        publishForSeats(state.seats) { actions }
    }

    context(Raise<SeatNotFound>)
    fun connectSeat(seat: Seat) {
        val game = repo.getBySeatUUID(seat.uuid.toString())
        val state = repo.getState(game)
        val player = playerAtPosition(seat.position, state.seats, state.allPlayers)!!
        val actions = playerJoinedActions(state, player, seat)

        repo.updateSeatStatus(seat, SeatStatus.CONNECTED)
        publishForSeats(state.seats) { actions }
    }

    private fun publishForSeats(seats: List<Seat>, action: (Seat) -> List<Action>) =
        seats.forEach { pubSub.publish(action.invoke(it), Channel("seat", it.uuid)) }

    /**
     * Controlling our game state. There are some special cases where the game engine is responsible
     * for the next action and not the user:
     *
     * - PLAY_CARD_BOT -> Wait, then play the card async
     * - NEW_TRICK -> Wait 1s before creating the new trick async
     * - NEW_TRICK -> Call gameLoop again, the next state could e.g. be PLAY_CARD_BOT
     * - NEW_HAND -> Call gameLoop again, the next state again could be a BOT action
     *
     * The delays make a better UX, so e.g a trick is not removed instantly by ClearPlayedCards action. The
     * other option would be to delay these actions client side, but I opted for this solution to keep the
     * client and server state as in-sync as possible.
     */
    context(Raise<GameError>)
    @OptIn(DelicateCoroutinesApi::class)
    private fun gameLoop(game: Game) {
        val updatedState = repo.getState(game)
        val currentHand = currentHand(updatedState.hands)!!
        val nextStateLoop = nextState(updatedState)

        when (nextStateLoop) {
            State.WAITING_FOR_PLAYERS -> {}
            State.FINISHED -> {
                publishForSeats(updatedState.seats) { listOf(Message("Game finished!")) }
            }

            State.PLAY_CARD -> {}
            State.TRUMP -> {}
            State.SCHIEBE -> {}
            State.PLAY_CARD_BOT -> GlobalScope.launch { delay(200).also { playAsBot(updatedState) } }
            State.TRUMP_BOT -> trumpAsBot(updatedState)
            State.SCHIEBE_BOT -> schiebeAsBot(updatedState)
            State.NEW_TRICK -> GlobalScope.launch {
                delay(1000)
                repo.createTrick(currentHand)
                val state = repo.getState(game)
                publishForSeats(updatedState.seats) { seat -> newTrickActions(state, seat) }
                gameLoop(game)
            }

            State.NEW_HAND -> GlobalScope.launch {
                delay(1000)
                val startingPlayer = nextHandStartingPlayer(
                    updatedState.hands,
                    updatedState.allPlayers,
                    updatedState.seats
                )
                val newHand = repo.createHand(NewHand(game, startingPlayer, randomHand()))
                repo.createTrick(newHand)
                val state = repo.getState(game)
                publishForSeats(updatedState.seats) { seat -> newHandActions(state, seat) }
                gameLoop(game)
            }
        }
    }

    context(Raise<GameError>)
    private fun trumpAsBot(state: GameState): GameState {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)!!
        if (!botPlayer.bot) {
            raise(PlayerIsNotBot(botPlayer, state))
        }

        // TODO: Get a good trump to choose
        val trump = chooseTrumpForBot(botPlayer, state)
        val request = ChooseTrumpRequest(state.game.uuid.toString(), trump.name)

        return trump(request, botPlayer)
    }

    context(Raise<GameError>)
    private fun playAsBot(state: GameState): GameState {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)!!
        if (!botPlayer.bot) {
            raise(PlayerIsNotBot(botPlayer, state))
        }

        // TODO: Get a good card to play
        val card = chooseCardForBot(botPlayer, state)
        val request = PlayCardRequest(
            state.game.uuid.toString(),
            PlayedCard(card.suit.toString(), card.rank.toString(), card.skin)
        )

        return play(request, botPlayer)
    }

    context(Raise<GameError>)
    private fun schiebeAsBot(state: GameState): GameState {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)!!
        if (!botPlayer.bot) {
            raise(PlayerIsNotBot(botPlayer, state))
        }

        // TODO: Better decision
        val gschobe = chooseGschobeForBot(botPlayer, state)
        val request = SchiebeRequest(state.game.uuid.toString(), gschobe.name)

        return schiebe(request, botPlayer)
    }
}