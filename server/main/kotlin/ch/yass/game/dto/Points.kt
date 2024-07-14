package ch.yass.game.dto

data class TotalPoints(val cardPoints: Int, val weisPoints: Int) {
    fun total(): Int {
        return cardPoints + weisPoints
    }
}
typealias Points = Map<Position, TotalPoints>
typealias SplitPoints = Map<Position, Int> // Either points by Cards or by Weis

