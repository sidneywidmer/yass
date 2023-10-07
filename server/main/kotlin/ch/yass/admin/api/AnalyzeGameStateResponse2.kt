package ch.yass.admin.api

import ch.yass.admin.api.analzye.Hand

data class AnalyzeGameStateResponse2(
    val hands: List<Hand>
)
