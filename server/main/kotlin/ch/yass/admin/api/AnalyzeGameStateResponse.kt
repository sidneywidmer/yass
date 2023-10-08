package ch.yass.admin.api

import ch.yass.admin.api.analzye.Hand

data class AnalyzeGameStateResponse(
    val hands: List<Hand>
)
