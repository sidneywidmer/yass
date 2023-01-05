package ch.yass.game.dto.db

import java.time.LocalDateTime
import java.util.*

data class Trick(
    val id: Int,
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)