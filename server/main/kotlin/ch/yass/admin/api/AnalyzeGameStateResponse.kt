package ch.yass.admin.api

import ch.yass.admin.api.analzye.Hand
import ch.yass.game.dto.Points

data class AnalyzeGameStateResponse(
    val hands: List<Hand>,
    val points: Points
)
