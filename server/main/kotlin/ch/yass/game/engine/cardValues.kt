package ch.yass.game.engine

import ch.yass.game.dto.Card
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Trump

fun cardValue(card: Card, trump: Trump): Int = when (trump) {
    Trump.CLUBS -> cardValueSuitTrump(card, trump)
    Trump.DIAMONDS -> cardValueSuitTrump(card, trump)
    Trump.HEARTS -> cardValueSuitTrump(card, trump)
    Trump.SPADES -> cardValueSuitTrump(card, trump)
    Trump.OBEABE -> cardValueObeabeTrump(card)
    Trump.UNEUFE -> cardValueUneufeTrump(card)
    else -> 0
}

fun cardValueSuitTrump(card: Card, trump: Trump): Int = when {
    trump.equalsSuit(card.suit) && card.rank == Rank.JACK -> 200 // Trump Buur
    trump.equalsSuit(card.suit) && card.rank == Rank.NINE -> 150 // Trump Nell
    trump.equalsSuit(card.suit) -> 100 + rankValueAscending(card.rank)
    else -> rankValueAscending(card.rank)
}

fun cardValueUneufeTrump(card: Card): Int = rankValueDescending(card.rank)

fun cardValueObeabeTrump(card: Card): Int = rankValueAscending(card.rank)

fun rankValueAscending(rank: Rank): Int = when (rank) {
    Rank.SIX -> 1
    Rank.SEVEN -> 2
    Rank.EIGHT -> 3
    Rank.NINE -> 4
    Rank.TEN -> 5
    Rank.JACK -> 6
    Rank.QUEEN -> 7
    Rank.KING -> 8
    Rank.ACE -> 9
}

fun rankValueDescending(rank: Rank): Int = when (rank) {
    Rank.SIX -> 9
    Rank.SEVEN -> 8
    Rank.EIGHT -> 7
    Rank.NINE -> 6
    Rank.TEN -> 5
    Rank.JACK -> 4
    Rank.QUEEN -> 3
    Rank.KING -> 2
    Rank.ACE -> 1
}

