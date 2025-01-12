package ch.yass.game.engine

import ch.yass.core.helper.cartesianProduct
import ch.yass.core.helper.toEnumMap
import ch.yass.game.dto.*
import java.util.EnumMap

/**
 * Get the order of a round based on a position. So if it's EAST's turn we
 * know the next player is SOUTH. I bet there's a million clever
 * implementations of this but probably none is as readable.
 */
fun positionsOrderedWithStart(position: Position): List<Position> {
    return when (position) {
        Position.NORTH -> listOf(Position.NORTH, Position.EAST, Position.SOUTH, Position.WEST)
        Position.EAST -> listOf(Position.EAST, Position.SOUTH, Position.WEST, Position.NORTH)
        Position.SOUTH -> listOf(Position.SOUTH, Position.WEST, Position.NORTH, Position.EAST)
        Position.WEST -> listOf(Position.WEST, Position.NORTH, Position.EAST, Position.SOUTH)
    }
}

fun deck(): List<Pair<Rank, Suit>> = cartesianProduct(Rank.regular(), Suit.regular()).shuffled()

/**
 * nextDeck allows us to inject a not so random deck in case we need it to replicate
 * errors or for testing.
 */
fun randomHand(forcedDeck: List<Card>?): EnumMap<Position, List<Card>> {
    val deck = forcedDeck?.map { Pair(it.rank, it.suit) } ?: deck()

    return mapOf(
        Position.NORTH to sort(deck.subList(0, 9).map { Card(it.second, it.first, "french") }),
        Position.EAST to sort(deck.subList(9, 18).map { Card(it.second, it.first, "french") }),
        Position.SOUTH to sort(deck.subList(18, 27).map { Card(it.second, it.first, "french") }),
        Position.WEST to sort(deck.subList(27, 36).map { Card(it.second, it.first, "french") }),
    ).toEnumMap()
}

fun sort(cards: List<Card>): List<Card> = cards.sortedWith(compareBy<Card> { it.suit }.thenBy { it.rank })

fun botName(position: Position): String = when (position) {
    Position.NORTH -> "Unit-N0R7H"
    Position.EAST -> "Bot-3000"
    Position.SOUTH -> "CyberS1X"
    Position.WEST -> "RoboW-X"
}
