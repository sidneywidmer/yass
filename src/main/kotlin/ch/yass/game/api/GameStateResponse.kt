package ch.yass.game.api

import arrow.core.Either
import arrow.core.continuations.either
import ch.yass.core.error.DomainError.*
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Player
import ch.yass.game.engine.currentHand
import ch.yass.game.engine.playerSeat
import java.time.LocalDateTime
import java.util.*

data class GameStateResponse(
    val gameUUID: UUID,
    val players: List<JoinGameResponsePlayer>,
    val trump: Trump?,
    val hand: List<Card>,
    val position: Position,
    val rejoinedAt: LocalDateTime?
) {
    companion object {
        fun from(state: GameState, player: Player): Either<UnexpectedError, GameStateResponse> = either.eager {
            val hand = currentHand(state.hands).toEither { UnexpectedError("current hand is empty") }.bind()
            val playerSeat = playerSeat(player, state.seats)
                .toEither { UnexpectedError("player ${player.uuid} has no seat") }
                .bind()

            GameStateResponse(
                state.game.uuid,
                JoinGameResponsePlayer.from(state),
                hand.trump,
                hand.cardsOf(playerSeat.position),
                playerSeat.position,
                playerSeat.rejoinedAt
            )
        }
    }
}

