package ch.yass.admin.api

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.db.*

/**
 * Helper to generate random hands. Can be used to generate data for
 * unittests.
 */
data class AnalyzeGameStateResponse(
    val game: Game,
    val allPlayers: List<Player>,
    val seats: List<Seat>,
    val hands: List<Hand>,
    val tricks: List<Trick>
) {
    companion object {
        fun from(state: GameState): AnalyzeGameStateResponse {
            return AnalyzeGameStateResponse(
                state.game,
                state.allPlayers,
                state.seats,
                state.hands,
                state.tricks
            )
        }
    }
}
