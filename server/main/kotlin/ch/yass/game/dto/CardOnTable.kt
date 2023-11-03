package ch.yass.game.dto

data class CardOnTable(
    val suit: Suit,
    val rank: Rank,
    val skin: String,
    val position: Position,
)
