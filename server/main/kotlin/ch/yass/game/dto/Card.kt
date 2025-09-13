package ch.yass.game.dto

import ch.yass.game.api.PlayedCard

data class Card(
    val suit: Suit,
    val rank: Rank,
    val skin: String,
) {
    companion object {
        fun from(card: PlayedCard): Card {
            return Card(
                Suit.valueOf(card.suit),
                Rank.valueOf(card.rank),
                card.skin,
            )
        }

        fun from(card: CardInHand): Card {
            return Card(card.suit, card.rank, card.skin)
        }
    }
}
