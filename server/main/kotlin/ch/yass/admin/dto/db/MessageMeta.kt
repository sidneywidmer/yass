package ch.yass.admin.dto.db

import java.util.*

data class MessageMeta(
    val path: String,
    val playerUuid: UUID,
    val serverVersion: String,
    val gameUuid: UUID?,
)
