package ch.yass.admin.dsl

import ch.yass.game.dto.Card
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Suit
import ch.yass.game.engine.notationToCard

fun interpretCard(card: String?): Card? {
    if (card == null) {
        return null
    }

    val suit = card.substring(1)
    val rank = card.first().toString()

    return notationToCard(suit, rank)
}

fun interpretCards(cards: String?): List<Card> {
    if (cards == null) {
        return emptyList()
    }

    // Special for welcome hand
    if (cards == "welcome") {
        return listOf(
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french"),
            Card(Suit.WELCOME, Rank.HELLO, "french")
        )
    }

    val cardList = cards.split(",")

    return cardList.mapNotNull { interpretCard(it) }
}