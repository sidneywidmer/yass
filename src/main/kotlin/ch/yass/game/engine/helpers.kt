package ch.yass.game.engine

import ch.yass.game.dto.Card
import ch.yass.game.dto.Position
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Suit

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

fun randomHand(): Map<Position, List<Card>> {
    val deck = Suit.values().zip(Rank.values()).shuffled()
    return mapOf(
        Position.NORTH to deck.subList(0, 8).map { Card(it.first, it.second, "french") },
        Position.EAST to deck.subList(9, 17).map { Card(it.first, it.second, "french") },
        Position.SOUTH to deck.subList(18, 26).map { Card(it.first, it.second, "french") },
        Position.WEST to deck.subList(27, 35).map { Card(it.first, it.second, "french") },
    )
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