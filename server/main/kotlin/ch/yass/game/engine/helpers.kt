package ch.yass.game.engine

import ch.yass.core.helper.cartesianProduct
import ch.yass.core.helper.toEnumMap
import ch.yass.game.dto.*
import java.util.EnumMap
import kotlin.random.Random

/**
 * Get the order of a round based on a position. So if it's EAST's turn we
 * know the next player is NORTH. I bet there's a million clever
 * implementations of this but probably none is as readable.
 */
fun positionsOrderedWithStart(position: Position): List<Position> {
    return when (position) {
        Position.NORTH -> listOf(Position.NORTH, Position.WEST, Position.SOUTH, Position.EAST)
        Position.EAST -> listOf(Position.EAST, Position.NORTH, Position.WEST, Position.SOUTH)
        Position.SOUTH -> listOf(Position.SOUTH, Position.EAST, Position.NORTH, Position.WEST)
        Position.WEST -> listOf(Position.WEST, Position.SOUTH, Position.EAST, Position.NORTH)
    }
}

/**
 * Given the lead and the currently active position, who still needs to play a card?
 */
fun upcomingPositions(lead: Position, current: Position): List<Position> {
    val orderedPositions = positionsOrderedWithStart(lead)
    val currentIndex = orderedPositions.indexOf(current)
    return orderedPositions.drop(currentIndex + 1)
}

fun deck(): List<Pair<Rank, Suit>> = cartesianProduct(Rank.regular(), Suit.regular()).shuffled()

fun allOfSuit(suit: Suit): List<Pair<Rank, Suit>> = cartesianProduct(Rank.regular(), listOf(suit))

/**
 * Deals a hand deterministically from [seed] and [handNumber] (each hand of a game draws from
 * the same seed but fast-forwards by handNumber draws, so hand 1, 2, 3... of a retried game
 * always deal the exact same cards), unless [forcedDeck] is given to replicate errors or for
 * testing.
 */
fun generateHand(seed: Int, handNumber: Int, forcedDeck: List<Card>? = null): EnumMap<Position, List<Card>> {
    val deck = forcedDeck?.map { Pair(it.rank, it.suit) } ?: seededDeck(seed, handNumber)

    return mapOf(
        Position.NORTH to sort(deck.subList(0, 9).map { Card(it.second, it.first) }),
        Position.WEST to sort(deck.subList(9, 18).map { Card(it.second, it.first) }),
        Position.SOUTH to sort(deck.subList(18, 27).map { Card(it.second, it.first) }),
        Position.EAST to sort(deck.subList(27, 36).map { Card(it.second, it.first) }),
    ).toEnumMap()
}

private fun seededDeck(seed: Int, handNumber: Int): List<Pair<Rank, Suit>> {
    val random = Random(seed)
    repeat(handNumber) { random.nextLong() }

    return cartesianProduct(Rank.regular(), Suit.regular()).shuffled(random)
}

fun sort(cards: List<Card>): List<Card> = cards.sortedWith(compareBy<Card> { it.suit }.thenBy { it.rank })

/**
* Sort by how high cards, the highest card is at index 0
*/
fun sortByPoints(cards: List<Card>, trump: Trump): List<Card> = cards.sortedWith(compareBy { cardValue(it, trump) }).reversed()

fun botName(position: Position): String = when (position) {
    Position.NORTH -> "Unit-N0R7H"
    Position.WEST -> "Bot-3000"
    Position.SOUTH -> "CyberS1X"
    Position.EAST -> "RoboW-X"
}

fun botId(position: Position): Int {
    return when (position) {
        Position.NORTH -> -1
        Position.WEST -> -2
        Position.SOUTH -> -3
        Position.EAST -> -4
    }
}