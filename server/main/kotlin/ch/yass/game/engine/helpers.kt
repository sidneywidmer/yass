package ch.yass.game.engine

import ch.yass.admin.dsl.interpretCards
import ch.yass.core.helper.cartesianProduct
import ch.yass.game.dto.*

fun regularRanks(): List<Rank> {
    return listOf(Rank.SIX, Rank.SEVEN, Rank.EIGHT, Rank.NINE, Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING, Rank.ACE)
}

fun regularSuits(): List<Suit> {
    return listOf(Suit.CLUBS, Suit.DIAMONDS, Suit.HEARTS, Suit.SPADES)
}

fun playableTrumps(): List<Trump> {
    return listOf(Trump.CLUBS, Trump.SPADES, Trump.HEARTS, Trump.DIAMONDS, Trump.UNEUFE, Trump.OBEABE)
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
//    return mapOf(
//        Position.NORTH to sort(deck.subList(0, 9).map { Card(it.second, it.first, "french") }),
//        Position.EAST to sort(deck.subList(9, 18).map { Card(it.second, it.first, "french") }),
//        Position.SOUTH to sort(deck.subList(18, 27).map { Card(it.second, it.first, "french") }),
//        Position.WEST to sort(deck.subList(27, 36).map { Card(it.second, it.first, "french") }),
//    )

    // Everyone with 9 blatt
//    return mapOf(
//        Position.NORTH to sort(interpretCards("S6,S7,S8,S9,S10,SJ,SQ,SK,SA")),
//        Position.EAST to sort(interpretCards("D6,D7,D8,D9,D10,DJ,DQ,DK,DA")),
//        Position.SOUTH to sort(interpretCards("C6,C7,C8,C9,C10,CJ,CQ,CK,CA")),
//        Position.WEST to sort(interpretCards("H6,H7,H8,H9,H10,HJ,HQ,HK,HA")),
//    )

    // North with 3 weise
    val c = interpretCards("S6,S7,S8,D6,D7,D8,C7,C8,C9")
    val newDeck = deck.filter { Card(it.second, it.first, "french") !in c }
    return mapOf(
        Position.NORTH to sort(interpretCards("S6,S7,S8,D6,D7,D8,C7,C8,C9")),
        Position.EAST to sort(newDeck.subList(0, 9).map { Card(it.second, it.first, "french") }),
        Position.SOUTH to sort(newDeck.subList(9, 18).map { Card(it.second, it.first, "french") }),
        Position.WEST to sort(newDeck.subList(18, 27).map { Card(it.second, it.first, "french") }),
    )
}

fun sort(cards: List<Card>): List<Card> {
    return cards.sortedWith(compareBy<Card> { it.suit }.thenBy { it.rank })
}