package ch.yass.game

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import ch.yass.core.error.*
import ch.yass.core.helper.logger
import ch.yass.game.api.ChooseTrumpRequest
import ch.yass.game.api.JoinGameRequest
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.api.internal.GameState
import ch.yass.game.api.internal.NewHand
import ch.yass.game.dto.Card
import ch.yass.game.dto.State
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.Player
import ch.yass.game.engine.*

class GameService(private val repo: GameRepository) {

    context(Raise<GameWithCodeNotFound>, Raise<GameAlreadyFull>)
    fun join(request: JoinGameRequest, player: Player): GameState {
        val game = repo.getByCode(request.code)

        repo.takeASeat(game, player)

        return repo.getState(game)
    }

    context(Raise<GameError>)
    fun play(request: PlayCardRequest, player: Player): GameState {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)
        val playedCard = Card.from(request.card)
        val nextState = nextState(state)

        ensure(expectedState(listOf(State.PLAY_CARD, State.PLAY_CARD_BOT), nextState)) {
            InvalidState(nextState, state)
        }
        ensure(playerHasTurn(player, state)) { PlayerIsLocked(player, state) }
        ensure(playerOwnsCard(player, playedCard, state)) { PlayerDoesNotOwnCard(player, playedCard, state) }
        ensure(isTrumpSet(currentHand(state.hands)!!)) { TrumpNotChosen(state) }
        ensure(cardIsPlayable(playedCard, player, state)) { CardNotPlayable(playedCard, player, state) }

        val currentTrick = currentTrick(state.tricks)!!
        val playerSeat = playerSeat(player, state.seats)!!

        logger().info("Player ${player.uuid} (bot:${player.bot}) played $playedCard")

        repo.playCard(playedCard, currentTrick, playerSeat)
        gameLoop(game)

        return repo.getState(game)
    }

    context(Raise<GameError>)
    fun trump(request: ChooseTrumpRequest, player: Player): GameState {
        val game = repo.getByUUID(request.game)
        val state = repo.getState(game)
        val chosenTrump = Trump.valueOf(request.trump)
        val nextState = nextState(state)
        val currentHand = currentHand(state.hands)!!

        ensure(expectedState(listOf(State.TRUMP, State.TRUMP_BOT), nextState)) { InvalidState(nextState, state) }
        ensure(playerHasTurn(player, state)) { PlayerIsLocked(player, state) }
        ensure(trumps().contains(chosenTrump)) { TrumpInvalid(chosenTrump) }

        logger().info("Player ${player.uuid} (bot:${player.bot}) choose trump $chosenTrump")

        repo.chooseTrump(chosenTrump, currentHand)
        gameLoop(game)

        return repo.getState(game)
    }

    context(Raise<GameError>)
    private fun gameLoop(game: Game) {
        do {
            val updatedState = repo.getState(game)
            val currentHand = currentHand(updatedState.hands)!!
            val nextStateLoop = nextState(updatedState)

            when (nextStateLoop) {
                State.FINISHED -> {}
                State.PLAY_CARD -> {}
                State.TRUMP -> {}
                State.PLAY_CARD_BOT -> playAsBot(updatedState)
                State.TRUMP_BOT -> trumpAsBot(updatedState)
                State.NEW_TRICK -> repo.createTrick(currentHand)
                State.NEW_HAND -> {
                    val startingPlayer = nextHandStartingPlayer(
                        updatedState.hands,
                        updatedState.allPlayers,
                        updatedState.seats
                    )
                    val newHand = repo.createHand(NewHand(game, startingPlayer, randomHand()))
                    repo.createTrick(newHand)
                }
            }
        } while (expectedState(listOf(State.NEW_TRICK, State.NEW_HAND), nextStateLoop))
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
}