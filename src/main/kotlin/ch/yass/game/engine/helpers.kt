package ch.yass.game.engine

import ch.yass.core.helper.cartesianProduct
import ch.yass.game.dto.Card
import ch.yass.game.dto.Position
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Suit
import kotlin.time.measureTime

fun ranks(): List<Rank> {
    return listOf(Rank.SIX, Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE)
}

fun suits(): List<Suit> {
    return listOf(Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES)
}

/**
 * Get the order of a round based on a position. So if it's EAST's turn we
 * know the next player is SOUTH. I bet there's a million clever
 * implementations of this but probably none is a simple.
 */
fun positionsOrderedWithStart(position: Position): List<Position> {
    return when (position) {
        Position.NORTH -> listOf(Position.NORTH, Position.EAST, Position.SOUTH, Position.WEST)
        Position.EAST -> listOf(Position.EAST, Position.SOUTH, Position.WEST, Position.NORTH)
        Position.SOUTH -> listOf(Position.SOUTH, Position.WEST, Position.NORTH, Position.EAST)
        Position.WEST -> listOf(Position.WEST, Position.NORTH, Position.EAST, Position.SOUTH)
    }
}

fun deck(): List<Pair<Rank, Suit>> = cartesianProduct(ranks(), suits()).shuffled()

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

fun welcomeHand(): Map<Position, List<Card>> {
    return mapOf(
        Position.NORTH to listOf(
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french")
        ),
        Position.EAST to listOf(
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french")
        ),
        Position.SOUTH to listOf(
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french")
        ),
        Position.WEST to listOf(
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french")
        ),
    )
}