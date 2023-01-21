package ch.yass.game

import arrow.core.Either
import arrow.core.continuations.either
import ch.yass.core.error.DomainError
import ch.yass.core.helper.logger
import ch.yass.game.api.JoinGameRequest
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayCardResponse
import ch.yass.game.api.internal.GameState
import ch.yass.game.api.internal.NewHand
import ch.yass.game.dto.Card
import ch.yass.game.dto.State
import ch.yass.game.dto.db.Player
import ch.yass.game.engine.*

class GameService(private val repo: GameRepository) {

    fun join(request: JoinGameRequest, player: Player): Either<DomainError, GameState> = either.eager {
        val maybeGame = repo.getByCode(request.code).bind()
        val game = maybeGame.toEither { DomainError.ValidationError("game.take-a-seat.empty") }.bind()

        repo.takeASeat(game, player).bind()

        repo.getState(game).bind()
    }

    fun play(request: PlayCardRequest, player: Player, recursion: Int = 0): Either<DomainError, PlayCardResponse> =
        either.eager {
            val game = repo.getByUUID(request.game).bind()
            val state = repo.getState(game).bind()
            val playedCard = Card.from(request.card)
            val response = PlayCardResponse(playedCard)
            val nextState = nextState(state)

            ensure(nextState == State.PLAY_CARD) {
                logger().error("Invalid game state, should have been ${State.PLAY_CARD} but was $nextState")
                DomainError.UnexpectedError("card.play.state.invalid")
            }
            ensure(playerHasTurn(player, state)) { DomainError.ValidationError("card.play.player.locked") }
            ensure(playerOwnsCard(player, playedCard, state)) { DomainError.ValidationError("card.play.not-owned") }
            ensure(cardIsPlayable(playedCard, player, state)) { DomainError.ValidationError("card.play.not-playable") }

            repo.playCard(playedCard, currentTrick(state), playerSeat(player, state)).bind()

            val updatedState = repo.getState(game).bind()
            when (nextState(updatedState)) {
                State.PLAY_CARD -> response
                State.NEW_TRICK -> {
                    repo.createTrick(currentHand(state)).bind()
                    response
                }

                State.NEW_HAND -> {
                    val startingPlayer = nextTrickStartingPlayer(updatedState)
                    val newHand = repo.createHand(NewHand(game, startingPlayer, randomHand())).bind()
                    repo.createTrick(newHand).bind()
                    response
                }

                else -> shift(DomainError.UnexpectedError("game.state.invalid"))
            }
        }
}