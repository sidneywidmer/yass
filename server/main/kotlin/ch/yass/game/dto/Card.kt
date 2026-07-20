package ch.yass.game.dto

import ch.yass.game.api.PlayedCard

data class Card(
    val suit: Suit,
    val rank: Rank,
) {
    companion object {
        fun from(card: PlayedCard): Card {
            return Card(
                Suit.valueOf(card.suit),
                Rank.valueOf(card.rank),
            )
        }

        fun from(card: CardInHand): Card = Card(card.suit, card.rank)
    }
}
