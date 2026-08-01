package ch.yass.game.api

import ch.yass.game.dto.RunningGame

data class GameInfoResponse(
    val runningGames: List<RunningGame>,
    val dailyChallengePlayed: Boolean
)
