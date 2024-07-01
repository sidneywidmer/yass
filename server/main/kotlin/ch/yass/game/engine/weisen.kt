package ch.yass.game.engine

import ch.yass.game.dto.Card
import ch.yass.game.dto.Rank

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

fun maxBlatt(cards: List<Card>): List<Card> {
    val foo = cards.groupBy { it.suit }.values.map { maxBlattBySuit(it) }
    return emptyList()
}

/**
 * Cards must only contain cards of one suit and be unique for this to work correctly.
 */
fun maxBlattBySuit(cards: List<Card>): List<Card> {
    val setOfCards = cards.toHashSet()

    var longest: List<Card> = listOf()
    for (card in cards) {
        if (!setOfCards.remove(card)) {
            continue
        }

        val sequence = mutableListOf<Card>()
        var counter: Int = rankMapOrdered[card.rank]!!
        while (setOfCards.removeIf { rankMapOrdered[it.rank]!! == counter-- }) {
            sequence.add(card)
        }

        // geht natürlich nicht, removeIf evaliert jedes element und counter geht jedes mal hoch
        counter = rankMapOrdered[card.rank]!!
        while (setOfCards.removeIf { rankMapOrdered[it.rank]!! == counter++ }) {
            sequence.add(card)
        }

        if (sequence.count() > longest.count()) {
            longest = sequence
        }
    }

    return longest
}
