package ch.yass.game.api.internal

import ch.yass.game.dto.db.*

data class GameState(
    val game: Game,
    val allPlayers: List<InternalPlayer>,
    val seats: List<Seat>,
    val hands: List<Hand>,
    val tricks: List<Trick>,
    val inMemory: Boolean = false
)