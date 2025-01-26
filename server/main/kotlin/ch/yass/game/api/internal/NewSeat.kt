package ch.yass.game.api.internal

import ch.yass.game.dto.Position
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.InternalPlayer

data class NewSeat(
    val game: Game,
    val player: InternalPlayer,
    val position: Position
)