package ch.yass.game.dto.db

import java.time.LocalDateTime
import java.util.*

data class Player(
    val id: Int,
    val uuid: UUID,
    val name: String,
    val bot: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
