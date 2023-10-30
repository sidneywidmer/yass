package ch.yass.game.dto

data class CardInHand(
    val suit: Suit,
    val rank: Rank,
    val skin: String,
    val locked: Boolean,
) {
    /*companion object {
        fun from(card: PlayedCard): CardInHand {
            return CardInHand(
                Suit.valueOf(card.suit),
                Rank.valueOf(card.rank),
                card.skin,
            )
        }
    }*/
}
