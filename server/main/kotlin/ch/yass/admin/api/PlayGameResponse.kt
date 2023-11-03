package ch.yass.admin.api

import ch.yass.admin.api.play.PlayGameSeat
import ch.yass.game.dto.CardOnTable
import java.util.*

data class PlayGameResponse(
    val gameUuid: UUID,
    val seats: List<PlayGameSeat>,
    val cardsPlayed: List<CardOnTable>
    -> display correctly in FE
    -> isCardPlayable doesnt account for if it is the players turn, account for this
)
