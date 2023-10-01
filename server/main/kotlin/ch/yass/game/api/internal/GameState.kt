package ch.yass.game.api.internal

import ch.yass.game.dto.db.*

data class GameState(
    val game: Game,
    val allPlayers: List<Player>,
    val seats: List<Seat>,
    val hands: List<Hand>,
    val tricks: List<Trick>,
)