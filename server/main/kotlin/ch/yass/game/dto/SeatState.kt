package ch.yass.game.dto

import ch.yass.game.dto.db.Player
import java.util.*

data class SeatState(
    val uuid: UUID,
    val cards: List<CardInHand>,
    val position: Position,
    val player: Player,
    val points: Points,
    val state: State,
    val activePosition: Position,
    val trump: Trump,
    val weise: List<WeisWithPoints>,
)