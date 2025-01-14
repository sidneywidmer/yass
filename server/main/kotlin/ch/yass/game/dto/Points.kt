package ch.yass.game.dto

import ch.yass.core.helper.associateWithToEnum
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Trick
import java.util.*

data class TotalPoints(val cardPoints: Int, val weisPoints: Int) {
    fun total(): Int = cardPoints + weisPoints
}
typealias Points = EnumMap<Position, TotalPoints>
typealias SplitPoints = EnumMap<Position, Int> // Either points by Cards or by Weis

data class HandWithTricks(val hand: Hand, val tricks: List<TrickWithPoints>)
data class TrickWithPoints(val lead: Position, val winner: Position, val points: Int, val trick: Trick)

fun List<HandWithTricks>.sumPointsByPosition(): SplitPoints =
    Position.entries.associateWithToEnum { position ->
        sumOf { hand -> hand.tricks.sumOf { trick -> if (trick.winner == position) trick.points else 0 } }
    }