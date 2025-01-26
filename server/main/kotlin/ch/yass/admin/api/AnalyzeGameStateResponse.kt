package ch.yass.admin.api

import ch.yass.admin.api.analzye.Hand
import ch.yass.game.dto.Points
import ch.yass.game.dto.TeamWithPoints
import java.util.*

data class AnalyzeGameStateResponse(
    val hands: List<Hand>,
    val points: Points,
    val gameUuid: UUID,
    val winners: TeamWithPoints,
    val losers: TeamWithPoints
)
