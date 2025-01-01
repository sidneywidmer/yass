package ch.yass.game.engine

import ch.yass.game.dto.Card
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Suit

val rankMap = mapOf(
    Rank.SIX to "6",
    Rank.SEVEN to "7",
    Rank.EIGHT to "8",
    Rank.NINE to "9",
    Rank.TEN to "10",
    Rank.JACK to "J",
    Rank.QUEEN to "Q",
    Rank.KING to "K",
    Rank.ACE to "A",
)

val suitMap = mapOf(
    Suit.SPADES to "S",
    Suit.HEARTS to "H",
    Suit.DIAMONDS to "D",
    Suit.CLUBS to "C",
    Suit.WELCOME to "W",
)

fun cardToNotation(card: Card): String = suitMap[card.suit] + rankMap[card.rank]

fun notationToCard(rank: String, suit: String): Card {
    val suitEnum = suitMap.filter { suit == it.value }.keys.firstOrNull()
    val rankEnum = rankMap.filter { rank == it.value }.keys.firstOrNull()

    require(suitEnum != null && rankEnum != null) { "Could not parse notation $rank$suit" }

    return Card(suitEnum, rankEnum, "french")
}