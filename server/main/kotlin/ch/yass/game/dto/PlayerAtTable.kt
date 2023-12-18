package ch.yass.game.dto

import java.util.*

data class PlayerAtTable(
    val uuid: UUID,
    val name: String,
    val bot: Boolean,
    val position: Position
)
