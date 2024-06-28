package ch.yass.game.dto.db

import java.time.LocalDateTime
import java.util.*

data class Player(
    val id: Int,
    val uuid: UUID,
    val oryUuid: UUID?, // Is null for anonymous user
    val name: String,
    val bot: Boolean,
    val anonToken: String?, // Is null for normal ory users (not anonymous)
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
