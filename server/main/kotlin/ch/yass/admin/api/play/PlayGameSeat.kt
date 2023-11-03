package ch.yass.admin.api.play

import ch.yass.game.dto.CardInHand
import ch.yass.game.dto.Position
import ch.yass.game.dto.db.Player
import java.util.*

data class PlayGameSeat(
    val uuid: UUID,
    val cards: List<CardInHand>,
    val position: Position,
    val player: Player
)