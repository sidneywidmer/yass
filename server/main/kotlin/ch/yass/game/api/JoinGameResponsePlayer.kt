package ch.yass.game.api

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.Position
import ch.yass.game.engine.currentTrick
import java.util.*

data class JoinGameResponsePlayer(
    val uuid: UUID,
    val name: String,
    val position: Position,
    val playedCard: Card? = null
) {
    companion object {
        fun from(state: GameState): List<JoinGameResponsePlayer> {
            return state.seats.map { seat ->
                val player = state.allPlayers.first { it.id == seat.playerId }
                val currentTrick = currentTrick(state.tricks)
                JoinGameResponsePlayer(
                    player.uuid,
                    player.name,
                    seat.position,
                    currentTrick?.cardOf(seat.position)
                )
            }
        }
    }
}