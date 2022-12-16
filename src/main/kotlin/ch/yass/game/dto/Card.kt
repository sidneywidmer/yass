package ch.yass.game.dto

data class Card(
    val suit: Suit,
    val rank: Rank,
    val skin: String? = null,
)
