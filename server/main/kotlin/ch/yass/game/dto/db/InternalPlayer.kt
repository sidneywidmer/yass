package ch.yass.game.dto.db

import java.time.LocalDateTime
import java.util.*

/**
 * It's essential that this dto never gets leaked since it possibly contains an anonToken
 */
data class InternalPlayer(
    val id: Int,
    val uuid: UUID,
    val oryUuid: UUID?, // Is null for anonymous user
    val name: String,
    val bot: Boolean,
    val anonToken: String?, // Is null for normal ory users (not anonymous)
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
