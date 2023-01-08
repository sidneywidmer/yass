package ch.yass.game.engine

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.Position
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Suit
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Player

/**
 * Check if the current player was ever dealt and has not yet
 * played * the given card.
 */
fun playerOwnsCard(player: Player, card: Card, state: GameState): Boolean {
    val hand = currentHand(state)
    val seat = playerSeat(player, state)

    val allCards = hand.cardsOf(seat.position)

    val tricks = state.tricks.filter { it.handId == hand.id }
    val playedCards = tricks.map { it.cardOf(seat.position) }
    val unplayedCards = allCards.minus(playedCards)

    return unplayedCards.any { it == card }
}


fun currentHand(state: GameState): Hand {
    return state.hands.first()
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