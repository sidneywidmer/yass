package ch.yass.game.api

import ch.yass.game.dto.CardOnTable
import ch.yass.game.dto.SeatState
import java.util.*

data class JoinGameResponse(
    val gameUuid: UUID,
    val code: String,
    val seat: SeatState,
    val cardsPlayed: List<CardOnTable>
)