package ch.yass.game.dto.db

import ch.yass.game.dto.Position
import java.time.LocalDateTime
import java.util.*

data class Seat(
    val id: Int,
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val playerId: Int,
    val gameId: Int,
    val position: Position,
    val rejoinedAt: LocalDateTime?,
)
