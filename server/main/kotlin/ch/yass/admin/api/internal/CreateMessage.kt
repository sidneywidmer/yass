package ch.yass.admin.api.internal

import ch.yass.game.dto.db.InternalPlayer

data class CreateMessage(
    val player: InternalPlayer,
    val gameUuid: String?,
    val message: String,
    val path: String,
    val serverVersion: String
)