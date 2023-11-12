package ch.yass.admin.api

import ch.yass.game.dto.SeatState
import ch.yass.game.dto.CardOnTable
import java.util.*

data class PlayGameResponse(
    val gameUuid: UUID,
    val seats: List<SeatState>,
    val cardsPlayed: List<CardOnTable>
)
