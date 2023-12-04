package ch.yass.game.dto

import ch.yass.game.api.CreateCustomGameRequest

data class GameSettings(
    val botNorth: Boolean,
    val botEast: Boolean,
    val botSouth: Boolean,
    val botWest: Boolean,
    val winningConditionType: WinningConditionType,
    val winningConditionValue: Int
) {
    companion object {
        fun from(request: CreateCustomGameRequest): GameSettings {
            return GameSettings(
                request.botNorth,
                request.botEast,
                request.botSouth,
                request.botWest,
                WinningConditionType.entries.first { it.name == request.winningConditionType },
                request.winningConditionValue
            )
        }
    }

    fun botPositions(): List<Position> {
        return mapOf(
            Position.NORTH to botNorth,
            Position.EAST to botEast,
            Position.SOUTH to botSouth,
            Position.WEST to botWest
        )
            .filter { it.value }
            .map { it.key }
    }
}