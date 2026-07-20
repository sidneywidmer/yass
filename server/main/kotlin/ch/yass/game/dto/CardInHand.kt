package ch.yass.game.dto

data class CardInHand(
    val suit: Suit,
    val rank: Rank,
    val state: CardInHandState,
)
