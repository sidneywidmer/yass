package ch.yass.dsl

import ch.yass.game.dto.Card
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Suit

fun interpretCard(card: String?): Card? {
    if (card == null) {
        return null
    }

    return Card(Suit.WELCOME, Rank.HELLO, "french")
}

fun interpretCards(cards: String?): List<Card> {
    if (cards == null) {
        return emptyList()
    }

    return listOf(
        Card(Suit.WELCOME, Rank.HELLO, "french"),
        Card(Suit.WELCOME, Rank.HELLO, "french"),
        Card(Suit.WELCOME, Rank.HELLO, "french"),
        Card(Suit.WELCOME, Rank.HELLO, "french")
    )
}