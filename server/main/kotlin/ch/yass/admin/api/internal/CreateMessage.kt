package ch.yass.admin.api.internal

import ch.yass.game.Game
import ch.yass.game.dto.Player

data class CreateMessage(
    val player: Player,
    val gameUuid: String,
    val message: String,
    val path: String,
    val serverVersion: String
)