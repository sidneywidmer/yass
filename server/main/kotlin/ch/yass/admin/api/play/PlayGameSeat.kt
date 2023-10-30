package ch.yass.admin.api.play

import ch.yass.game.dto.Card
import ch.yass.game.dto.Position
import ch.yass.game.dto.db.Player
import java.util.UUID

data class PlayGameSeat(
    val uuid: UUID,
    val cards: List<Card>,
    val position: Position,
    val player: Player
)