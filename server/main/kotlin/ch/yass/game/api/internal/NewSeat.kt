package ch.yass.game.api.internal

import ch.yass.game.dto.Position
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.Player

data class NewSeat(
    val game: Game,
    val player: Player,
    val position: Position
)