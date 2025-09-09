package ch.yass.game.bot

import ch.yass.game.dto.Card
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Suit
import ch.yass.game.dto.Trump

data class TrumpCandidate(val trump: Trump, val reason: TrumpReason)

sealed interface TrumpReason {
    val weight: Int
    fun check(cards: List<Card>, trump: Trump): Boolean

    fun List<Card>.trumpCards(trump: Trump): List<Card> =
        trump.toSuit()?.let { suit -> filter { it.suit == suit } } ?: emptyList()
}

object PerfectTrio : TrumpReason {
    override val weight = 10
    override fun check(cards: List<Card>, trump: Trump) =
        cards.trumpCards(trump)
            .map { it.rank }
            .toSet()
            .containsAll(setOf(Rank.JACK, Rank.NINE, Rank.ACE))
}

object JackNineWithTwoAdditional : TrumpReason {
    override val weight = 9
    override fun check(cards: List<Card>, trump: Trump) =
        cards.trumpCards(trump)
            .let { trumps ->
                val ranks = trumps.map { it.rank }.toSet()
                ranks.containsAll(setOf(Rank.JACK, Rank.NINE)) && trumps.size >= 4
            }
}

object JackWithThreeOrMoreTrumps : TrumpReason {
    override val weight = 7
    override fun check(cards: List<Card>, trump: Trump) =
        cards.trumpCards(trump)
            .let { trumps -> trumps.any { it.rank == Rank.JACK } && trumps.size >= 4 }
}

object SixPlusTrumpsWithPowerCard : TrumpReason {
    override val weight = 6
    override fun check(cards: List<Card>, trump: Trump) =
        cards.trumpCards(trump)
            .let { trumps -> trumps.size >= 6 && trumps.any { it.rank in setOf(Rank.JACK, Rank.NINE, Rank.ACE) } }
}

object NineWithFiveOrMoreTrumps : TrumpReason {
    override val weight = 6
    override fun check(cards: List<Card>, trump: Trump) =
        cards.trumpCards(trump)
            .let { trumps -> trumps.any { it.rank == Rank.NINE } && trumps.size >= 5 }
}

object NineJackStrongSides : TrumpReason {
    override val weight = 5
    override fun check(cards: List<Card>, trump: Trump): Boolean {
        val trumpCards = cards.trumpCards(trump)
        val nonTrumpCards = cards - trumpCards.toSet()

        val trumpRanks = trumpCards.map { it.rank }.toSet()
        val hasNineAndJack = trumpRanks.contains(Rank.NINE) && trumpRanks.contains(Rank.JACK)
        val hasAdditionalTrump = trumpCards.size >= 3

        val strongSides = nonTrumpCards.count { it.rank in setOf(Rank.ACE, Rank.KING, Rank.QUEEN) }

        return hasNineAndJack && hasAdditionalTrump && strongSides >= 2
    }
}

object JackWithTwoTrumps : TrumpReason {
    override val weight = 4
    override fun check(cards: List<Card>, trump: Trump) =
        cards.trumpCards(trump)
            .let { trumps -> trumps.any { it.rank == Rank.JACK } && trumps.size == 3 }
}

object FiveTrumpsWithAce : TrumpReason {
    override val weight = 3
    override fun check(cards: List<Card>, trump: Trump) =
        cards.trumpCards(trump)
            .let { trumps ->
                val ranks = trumps.map { it.rank }.toSet()
                trumps.size >= 5 && ranks.contains(Rank.ACE) &&
                        !ranks.contains(Rank.JACK) && !ranks.contains(Rank.NINE)
            }
}

object NineWithFourTrumps : TrumpReason {
    override val weight = 3
    override fun check(cards: List<Card>, trump: Trump) =
        cards.trumpCards(trump)
            .let { trumps -> trumps.any { it.rank == Rank.NINE } && trumps.size >= 4 }
}

// In case no other TrumpReason matches, you'll get this one
object Fallback : TrumpReason {
    override val weight = 0
    override fun check(cards: List<Card>, trump: Trump) = true
}

object StrongUneufe : TrumpReason {
    override val weight = 8
    override fun check(cards: List<Card>, trump: Trump): Boolean {
        if (trump != Trump.UNEUFE) return false

        val sixes = cards.count { it.rank == Rank.SIX }
        val lowCards = cards.count { it.rank in setOf(Rank.SIX, Rank.SEVEN, Rank.EIGHT) }
        val highCards = cards.count { it.rank in setOf(Rank.ACE, Rank.KING, Rank.QUEEN) }
        val hasWall = Suit.regular().any { suit ->
            val suitCards = cards.filter { it.suit == suit }.map { it.rank }.toSet()
            suitCards.containsAll(setOf(Rank.SIX, Rank.SEVEN, Rank.EIGHT))
        }

        return sixes >= 2 && lowCards >= 5 && highCards <= 2 && hasWall
    }
}

object StrongObenabe : TrumpReason {
    override val weight = 8
    override fun check(cards: List<Card>, trump: Trump): Boolean {
        if (trump != Trump.OBEABE) return false

        val aces = cards.count { it.rank == Rank.ACE }
        val highCards = cards.count { it.rank in setOf(Rank.ACE, Rank.KING, Rank.QUEEN) }
        val lowCards = cards.count { it.rank in setOf(Rank.SIX, Rank.SEVEN, Rank.EIGHT, Rank.NINE) }
        val hasWall = Suit.regular().any { suit ->
            val suitCards = cards.filter { it.suit == suit }.map { it.rank }.toSet()
            suitCards.containsAll(setOf(Rank.ACE, Rank.KING, Rank.QUEEN))
        }

        return aces >= 2 && highCards >= 5 && lowCards <= 2 && hasWall
    }
}

val trumpReasons = listOf(
    JackWithThreeOrMoreTrumps,
    PerfectTrio,
    JackNineWithTwoAdditional,
    NineWithFiveOrMoreTrumps,
    SixPlusTrumpsWithPowerCard,
    JackWithTwoTrumps,
    NineWithFourTrumps,
    FiveTrumpsWithAce,
    NineJackStrongSides,
    Fallback,
    StrongUneufe,
    StrongObenabe,
)

/**
 * Gets the best candidate, but keep in mind that it can be a bad choice with `Fallback` as reason.
 */
fun getTrumpCandidate(cards: List<Card>): TrumpCandidate {
    return Trump.playable()
        .map { trump -> TrumpCandidate(trump, trumpReasons.filter { it.check(cards, trump) }.maxBy { it.weight }) }
        .maxBy { it.reason.weight }
}
