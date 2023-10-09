package ch.yass.game.engine

import ch.yass.core.helper.cartesianProduct
import ch.yass.game.dto.*

fun regularRanks(): List<Rank> {
    return listOf(Rank.SIX, Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE)
}

fun regularSuits(): List<Suit> {
    return listOf(Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES)
}

fun regularTrumps(): List<Trump> {
    // TODO: Add back Trump.UNEUFE, Trump.OBEABE
    return listOf(Trump.CLUBS, Trump.SPADES, Trump.HEARTS, Trump.DIAMONDS)
}

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

fun deck(): List<Pair<Rank, Suit>> = cartesianProduct(regularRanks(), regularSuits()).shuffled()

fun randomHand(): Map<Position, List<Card>> {
    val deck = deck()
    return mapOf(
        Position.NORTH to sort(deck.subList(0, 9).map { Card(it.second, it.first, "french") }),
        Position.EAST to sort(deck.subList(9, 18).map { Card(it.second, it.first, "french") }),
        Position.SOUTH to sort(deck.subList(18, 27).map { Card(it.second, it.first, "french") }),
        Position.WEST to sort(deck.subList(27, 36).map { Card(it.second, it.first, "french") }),
    )
}

fun sort(cards: List<Card>): List<Card> {
    return cards.sortedWith(compareBy<Card> { it.suit }.thenBy { it.rank })
}