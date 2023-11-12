package ch.yass.game.dto

data class CardInHand(
    val suit: Suit,
    val rank: Rank,
    val skin: String,
    val locked: Boolean,
)
