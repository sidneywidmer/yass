package ch.yass.admin.dsl

import ch.yass.game.dto.Card
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

    val cardList = cards.split(",")

    return cardList.mapNotNull { interpretCard(it) }
}