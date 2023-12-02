package ch.yass.game

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import ch.yass.core.error.*
import ch.yass.core.pubsub.Action
import ch.yass.core.pubsub.Channel
import ch.yass.core.pubsub.PubSub
import ch.yass.game.api.*
import ch.yass.game.api.internal.GameState
import ch.yass.game.api.internal.NewHand
import ch.yass.game.dto.Card
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.State
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.*
import ch.yass.game.pubsub.*
import kotlinx.coroutines.*

class GameService(private val repo: GameRepository, private val pubSub: PubSub) {

    context(Raise<GameWithCodeNotFound>, Raise<GameAlreadyFull>)
    fun join(request: JoinGameRequest, player: Player): GameState {
        val game = repo.getByCode(request.code)

        repo.takeASeat(game, player)

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

        publishForSeats(state.seats) { seat -> trumpChosenActions(repo.getState(game), chosenTrump, seat) }

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

        publishForSeats(state.seats) { seat -> schiebeActions(repo.getState(game), seat) }

        gameLoop(game)

        return repo.getState(game)
    }

    /**
     * Controlling our game state. There are some special cases where the game engine is responsible
     * for the next action and not the user:
     *
     * - PLAY_CARD_BOT -> Wait for 1.5s then play the card async
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
                publishForSeats(updatedState.seats) { seat -> newTrickActions(repo.getState(game), seat) }
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
                publishForSeats(updatedState.seats) { seat -> newHandActions(repo.getState(game), seat) }
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

    private fun publishForSeats(seats: List<Seat>, action: (Seat) -> List<Action>) =
        seats.forEach { pubSub.publish(action.invoke(it), Channel("seat", it.uuid)) }

}