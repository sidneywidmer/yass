package ch.yass.game.dto

import java.time.LocalDateTime
import java.util.*

data class Seat(
    val id: Int,
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val player: Player,
    val game: Game,
    val position: Position,
)
