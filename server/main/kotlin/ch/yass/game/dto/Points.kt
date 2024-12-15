package ch.yass.game.dto

import java.util.EnumMap

data class TotalPoints(val cardPoints: Int, val weisPoints: Int) {
    fun total(): Int {
        return cardPoints + weisPoints
    }
}
typealias Points = EnumMap<Position, TotalPoints>
typealias SplitPoints = EnumMap<Position, Int> // Either points by Cards or by Weis

