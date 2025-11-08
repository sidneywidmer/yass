package ch.yass.game.dto

data class CardOnTable(
    val suit: Suit,
    val rank: Rank,
    val skin: Skin,
    val position: Position,
)
