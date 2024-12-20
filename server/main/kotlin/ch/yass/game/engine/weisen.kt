package ch.yass.game.engine

import ch.yass.game.dto.*

fun stoeckWeis(cards: List<Card>, trump: Trump): List<Weis> {
    val queen = cards.firstOrNull { it.suit.name == trump.name && it.rank == Rank.QUEEN }
    val king = cards.firstOrNull { it.suit.name == trump.name && it.rank == Rank.KING }

    if (queen == null || king == null) {
        return emptyList()
    }

    val match = listOf(king, queen)
    return listOf(Weis(WeisType.STOECK, match))
}

/**
 * Find cards of the same rank, e.g. 4 7's
 */
fun gleicheWeise(cards: List<Card>): List<Weis> =
        cards.groupBy { it.rank }.values
                .filter { it.size == 4 }
                .map { Weis(weisType(it), it) }

/**
 * Get the longest sequence (e.g. 4 blatt) for each suit. Must be at least 3 cards in a row.
 */
fun blattWeise(cards: List<Card>): List<Weis> =
        cards.groupBy { it.suit }.values
                .map { maxBlattBySuit(it) }
                .filter { it.size >= 3 }
                .map { Weis(weisType(it), it) }

/**
 * We only check for blatt length and some basic indicators to figure out what WeisType is given, so the cards
 * already must represent a valid weis. In case they are not something went terrible wrong and we throw.
 */
fun weisType(cards: List<Card>): WeisType {
    val fourOfTheSame = cards.size == 4 && cards.all { it.rank == cards[0].rank }
    return when {
        fourOfTheSame && cards[0].rank == Rank.JACK -> WeisType.VIER_BUUR
        fourOfTheSame && cards[0].rank == Rank.NINE -> WeisType.VIER_NELL
        fourOfTheSame -> WeisType.VIER_GLEICHE
        cards.size == 3 -> WeisType.DREI_BLATT
        cards.size == 4 -> WeisType.VIER_BLATT
        cards.size == 5 -> WeisType.FUENF_BLATT
        cards.size == 6 -> WeisType.SECHS_BLATT
        cards.size == 7 -> WeisType.SIEBEN_BLATT
        cards.size == 8 -> WeisType.ACHT_BLATT
        cards.size == 9 -> WeisType.NEUN_BLATT
        else -> throw IllegalArgumentException("invalid list of cards for weis type evaluation given")
    }
}

/**
 * Cards must only contain cards of one suit and be unique for this to work correctly.
 */
fun maxBlattBySuit(cards: List<Card>): List<Card> {
    val setOfCards = cards.associateBy { rankValueAscending(it.rank) }.toMutableMap()

    var longest: List<Card> = listOf()
    for (card in cards) {
        if (longest.contains(card)) continue

        val sequence = mutableListOf<Card>()
        sequence.add(card)
        setOfCards.remove(rankValueAscending(card.rank))

        var counter: Int = rankValueAscending(card.rank) - 1
        while (setOfCards.contains(counter)) {
            sequence.add(setOfCards.getValue(counter))
            setOfCards.remove(counter)
            counter--
        }

        counter = rankValueAscending(card.rank) + 1
        while (setOfCards.contains(counter)) {
            sequence.add(setOfCards.getValue(counter))
            setOfCards.remove(counter)
            counter++
        }

        if (sequence.count() > longest.count()) {
            longest = sequence
        }
    }

    return longest
}
