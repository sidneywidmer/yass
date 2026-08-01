package ch.yass.game.api

import ch.yass.game.dto.DailyLeaderboardEntry
import java.time.LocalDate

data class DailyLeaderboardResponse(
    val day: LocalDate,
    val entries: List<DailyLeaderboardEntry>
)
