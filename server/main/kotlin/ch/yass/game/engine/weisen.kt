package ch.yass.game.engine

import ch.yass.game.dto.*

private var rankMapOrdered: Map<Rank, Int> = mapOf(
        Rank.SIX to 6,
        Rank.SEVEN to 7,
        Rank.EIGHT to 8,
        Rank.NINE to 9,
        Rank.TEN to 10,
        Rank.JACK to 11,
        Rank.QUEEN to 12,
        Rank.KING to 13,
        Rank.ACE to 14,
)

fun stoeckWeis(cards: List<Card>, trump: Trump): List<Weis> {
    val queen = cards.firstOrNull { it.suit.name == trump.name && it.rank == Rank.QUEEN }
    val king = cards.firstOrNull { it.suit.name == trump.name && it.rank == Rank.KING }

    if (queen == null || king == null) {
        return emptyList()
    }

    val match = listOf(king, queen)
    return listOf(Weis(WeisType.STOECK, match))
}

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
    require(cards.size >= 3) { "invalid list of cards for weis type evaluation given" } // should never happen

    val fourOfTheSame = cards.size == 4 && cards.all { it.rank == cards[0].rank }

    if (fourOfTheSame && cards[0].rank == Rank.JACK) {
        return WeisType.VIER_BUUR
    }

    if (fourOfTheSame && cards[0].rank == Rank.NINE) {
        return WeisType.VIER_NELL
    }

    if (fourOfTheSame) {
        return WeisType.VIER_GLEICHE
    }

    return when (cards.size) {
        3 -> WeisType.DREI_BLATT
        4 -> WeisType.VIER_BLATT
        5 -> WeisType.FUENF_BLATT
        6 -> WeisType.SECHS_BLATT
        7 -> WeisType.SIEBEN_BLATT
        8 -> WeisType.ACHT_BLATT
        9 -> WeisType.NEUN_BLATT
        else -> throw IllegalArgumentException("invalid list of cards for weis type evaluation given")
    }
}

/**
 * Cards must only contain cards of one suit and be unique for this to work correctly.
 */
fun maxBlattBySuit(cards: List<Card>): List<Card> {
    val setOfCards = cards.associateBy { rankMapOrdered[it.rank]!! }.toMutableMap()

    var longest: List<Card> = listOf()
    for (card in cards) {
        if (longest.contains(card)) continue

        val sequence = mutableListOf<Card>()
        sequence.add(card)
        setOfCards.remove(rankMapOrdered[card.rank]!!)

        var counter: Int = rankMapOrdered[card.rank]!! - 1
        while (setOfCards.contains(counter)) {
            sequence.add(setOfCards[counter]!!)
            setOfCards.remove(counter)
            counter--
        }

        counter = rankMapOrdered[card.rank]!! + 1
        while (setOfCards.contains(counter)) {
            sequence.add(setOfCards[counter]!!)
            setOfCards.remove(counter)
            counter++
        }

        if (sequence.count() > longest.count()) {
            longest = sequence
        }
    }

    return longest
}
