package ch.yass.admin.api

import ch.yass.admin.api.analzye.Hand
import ch.yass.game.dto.Points
import java.util.UUID

data class AnalyzeGameStateResponse(
    val hands: List<Hand>,
    val points: Points,
    val gameUuid: UUID
)
