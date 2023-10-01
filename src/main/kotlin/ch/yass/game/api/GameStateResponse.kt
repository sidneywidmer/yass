package ch.yass.game.api

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
        fun from(state: GameState, player: Player): GameStateResponse {
            val hand = currentHand(state.hands)!!
            val playerSeat = playerSeat(player, state.seats)

            return GameStateResponse(
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

