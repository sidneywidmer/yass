package ch.yass.auth.dto

import java.time.LocalDateTime
import java.util.*

data class Player(
    val id: Int,
    val uuid: UUID,
    val name: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
