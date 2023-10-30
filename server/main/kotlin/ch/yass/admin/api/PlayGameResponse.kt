package ch.yass.admin.api

import ch.yass.admin.api.play.PlayGameSeat
import java.util.*

data class PlayGameResponse(
    val gameUuid: UUID,
    val seats: List<PlayGameSeat>
)
