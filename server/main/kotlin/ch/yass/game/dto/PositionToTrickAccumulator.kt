package ch.yass.game.dto

import ch.yass.game.dto.db.Trick

data class PositionToTrickAccumulator(
    val positions: Map<Position, List<Trick>>,
    val lead: Position
)
