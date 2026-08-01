package ch.yass.game.api.internal

import ch.yass.game.dto.Position
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Trick

data class DailyGame(
    val game: Game,
    val player: String,
    val position: Position,
    val hands: List<Hand>,
    val tricks: List<Trick>
)
