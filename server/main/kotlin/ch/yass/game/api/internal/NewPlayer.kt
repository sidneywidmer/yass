package ch.yass.game.api.internal

import java.util.UUID

data class NewPlayer(
    val uuid: UUID,
    val name: String,
)
