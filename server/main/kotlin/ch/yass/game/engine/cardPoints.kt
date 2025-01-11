package ch.yass.game.engine

import ch.yass.game.dto.Card
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Trump

fun multiplyByTrump(points: Int, trump: Trump): Int {
    return when (trump) {
        Trump.OBEABE -> points * 3
        Trump.UNEUFE -> points * 3
        Trump.DIAMONDS -> points * 2
        Trump.HEARTS -> points * 2
        Trump.CLUBS -> points
        Trump.SPADES -> points
        else -> points
    }
}

fun cardPoints(card: Card, trump: Trump): Int {
    return when (trump) {
        Trump.DIAMONDS -> cardPointsSuitTrump(card, trump)
        Trump.HEARTS -> cardPointsSuitTrump(card, trump)
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
}