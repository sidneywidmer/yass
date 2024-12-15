package ch.yass.game.dto

import ch.yass.game.dto.db.Trick
import java.util.EnumMap

data class PositionToTrickAccumulator(
    val positions: EnumMap<Position, List<Trick>>,
    val lead: Position
)
