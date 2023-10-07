package ch.yass.game.api

import ch.yass.game.api.internal.GameState
import ch.yass.game.engine.currentTrick

data class PlayedCard(
    val suit: String,
    val rank: String,
    val skin: String,
){
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