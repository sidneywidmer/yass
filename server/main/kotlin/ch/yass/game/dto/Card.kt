package ch.yass.game.dto

import ch.yass.game.api.PlayedCard

data class Card(
    val suit: Suit,
    val rank: Rank,
    val skin: Skin = Skin.DEFAULT,
) {
    companion object {
        fun from(card: PlayedCard): Card {
            return Card(
                Suit.valueOf(card.suit),
                Rank.valueOf(card.rank),
                Skin.valueOf(card.skin ?: Skin.DEFAULT.name),
            )
        }

        fun from(card: CardInHand): Card {
            return Card(card.suit, card.rank, card.skin)
        }
    }
}
