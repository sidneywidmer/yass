package ch.yass.game.dto

import ch.yass.auth.dto.Player
import java.time.LocalDateTime
import java.util.*

data class Hand(
    val id: Int,
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val startingPlayer: Player,
    val game: Game,
    val trump: String,
    val gschobe: Boolean,
    val points: Int,
)
