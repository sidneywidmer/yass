package ch.yass.admin.api.analzye

import ch.yass.game.dto.Card
import ch.yass.game.dto.Position
import java.util.UUID

data class PlayerWithCards(
    val uuid: UUID,
    val name: String,
    val cards: List<Card>,
    val position: Position
)
