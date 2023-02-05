package ch.yass.game

import arrow.core.Either
import arrow.core.continuations.either
import ch.yass.core.error.DomainError
import ch.yass.core.error.DomainError.*
import ch.yass.core.helper.logger
import ch.yass.game.api.JoinGameRequest
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.internal.GameState
import ch.yass.game.api.internal.NewHand
import ch.yass.game.dto.Card
import ch.yass.game.dto.State
import ch.yass.game.dto.db.Player
import ch.yass.game.engine.*

class GameService(private val repo: GameRepository) {

    fun join(request: JoinGameRequest, player: Player): Either<DomainError, GameState> = either.eager {
        val maybeGame = repo.getByCode(request.code).bind()
        val game = maybeGame.toEither { ValidationError("game.take-a-seat.empty") }.bind()

        repo.takeASeat(game, player).bind()
        repo.getState(game).bind()
    }

    fun play(request: PlayCardRequest, player: Player): Either<DomainError, GameState> = either.eager {
        val game = repo.getByUUID(request.game).bind()
        val state = repo.getState(game).bind()
        val playedCard = Card.from(request.card)
        val nextState = nextState(state)

        ensure(nextState == State.PLAY_CARD) {
            UnexpectedError("invalid game state, should have been ${State.PLAY_CARD} but was $nextState")
        }
        ensure(playerHasTurn(player, state).bind()) { ValidationError("play.player.locked") }
        ensure(playerOwnsCard(player, playedCard, state).bind()) { ValidationError("play.not-owned") }
        ensure(cardIsPlayable(playedCard, player, state).bind()) { ValidationError("play.not-playable") }

        val currentTrick = currentTrick(state.tricks).bind { UnexpectedError("current trick is empty") }
        val playerSeat = playerSeat(player, state.seats).bind { UnexpectedError("player seat is empty") }

        repo.playCard(playedCard, currentTrick, playerSeat).bind()

        val updatedState = repo.getState(game).bind()
        val currentHand = currentHand(updatedState.hands).bind { UnexpectedError("current hand is empty") }
        when (nextState(updatedState)) {
            State.PLAY_CARD -> {}
            State.NEW_TRICK -> repo.createTrick(currentHand).bind()
            State.NEW_HAND -> {
                val startingPlayer = nextTrickStartingPlayer(
                    updatedState.hands,
                    updatedState.allPlayers,
                    updatedState.seats
                ).bind { UnexpectedError("starting player is empty") }
                val newHand = repo.createHand(NewHand(game, startingPlayer, randomHand())).bind()
                repo.createTrick(newHand).bind()
            }
        }

        repo.getState(game).bind()
    }
}