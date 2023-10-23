package ch.yass.game.engine

import ch.yass.game.dto.Card
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Trump

fun cardPoints(card: Card, trump: Trump): Int {
    return when (trump) {
        Trump.DIAMONDS -> cardPointsSuitTrump(card, trump) * 2
        Trump.HEARTS -> cardPointsSuitTrump(card, trump) * 2
        Trump.CLUBS -> cardPointsSuitTrump(card, trump)
        Trump.SPADES -> cardPointsSuitTrump(card, trump)
        Trump.OBEABE -> cardPointsObeabeTrump(card.rank)
        Trump.UNEUFE -> cardPointsUneufeTrump(card.rank)
        else -> 0
    }
}

fun cardPointsSuitTrump(card: Card, trump: Trump): Int {
    return when {
        trump.equalsSuit(card.suit) && card.rank == Rank.JACK -> 20 // Trump Buur
        trump.equalsSuit(card.suit) && card.rank == Rank.NINE -> 14 // Trump Nell
        else -> rankPointsSuitTrump(card.rank)
    }
}

fun rankPointsSuitTrump(rank: Rank): Int = when (rank) {
    Rank.SIX -> 0
    Rank.SEVEN -> 0
    Rank.EIGHT -> 0
    Rank.NINE -> 0
    Rank.TEN -> 10
    Rank.JACK -> 2
    Rank.QUEEN -> 3
    Rank.KING -> 4
    Rank.ACE -> 11
    else -> 0
}


fun cardPointsObeabeTrump(rank: Rank) = when (rank) {
    Rank.SIX -> 0
    Rank.SEVEN -> 0
    Rank.EIGHT -> 8
    Rank.NINE -> 0
    Rank.TEN -> 10
    Rank.JACK -> 2
    Rank.QUEEN -> 3
    Rank.KING -> 4
    Rank.ACE -> 11
    else -> 0
}

fun cardPointsUneufeTrump(rank: Rank): Int = when (rank) {
    Rank.SIX -> 11
    Rank.SEVEN -> 0
    Rank.EIGHT -> 8
    Rank.NINE -> 0
    Rank.TEN -> 10
    Rank.JACK -> 2
    Rank.QUEEN -> 3
    Rank.KING -> 4
    Rank.ACE -> 0
    else -> 0
}