package ch.yass.game.bot

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.CardInHand
import ch.yass.game.dto.CardInHandState
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.InternalPlayer
import ch.yass.game.dto.db.Trick
import ch.yass.game.engine.*

// https://www.watson.ch/schweiz/sport/293024792-jass-tipps-welche-karte-wuerdest-du-hier-spielen
// https://www.swisslos.ch/de/jass/informationen/tipps-vom-jass-onkel/schieber-jass.html
// https://s1403f1212affd0cf.jimcontent.com/download/version/1653654748/module/8968421285/name/Tipps%20Schieber1910.pdf
// https://www.earthli.com/jass/manual.php?page=strategies


data class PlayCandidate(val card: Card, val reasons: List<PlayReason>)

data class PlayParams(
    val hand: Hand,
    val trick: Trick,
    val playable: List<CardInHand>,
    val lead: Position,
    val state: GameState,
    val notGschobeOpeningLead: Boolean // First card in the first trick of an ungschobe hand
//    val points: Int,
//    val partnerWinning: Boolean,
//    val bock: List<Card>,
//    val remainingTrumps: List<Card>,
)

sealed interface PlayReason {
    val weight: Int

    /**
     * card: The card that is currently being evaluated
     * params: Facts about the current game state
     * candidates: All PLAYABLE cards with reasons why they should be played
     */
    fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean

    fun List<PlayCandidate>.reasonsOf(card: Card): List<PlayReason> = first { it.card == card }.reasons
}

/**
 * Check if this is the lowest card of that suit which can win the trick. Only relevant for
 * last card.
 */
object CanWinTrickWithLowest : PlayReason {
    override val weight = 2
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        // If we can't win the trick with given card return
        if (!candidates.reasonsOf(card).contains(CanWinTrick)) return false

        // Get the lowest card that can win the trick of the same suit as given card
        val lowestOfSuit = candidates
            .filter { it.reasons.contains(CanWinTrick) }
            .filter { it.card.suit == card.suit }
            .map { it.card }
            .minBy { it.rank }

        return lowestOfSuit == card
    }
}

/**
 * Check if we can win the trick without using a trump. Only relevant for last card.
 */
object CanWinTrickWithNoTrump : PlayReason {
    override val weight = 5
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        // If we can't win the trick with given card return
        if (!candidates.reasonsOf(card).contains(CanWinTrick)) return false
        if (card.suit != params.hand.trump.toSuit()) return true
        return false
    }
}

/**
 * If this is the last card played, check if we have an easy way to win the trick.
 */
object CanWinTrick : PlayReason {
    override val weight = 2
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        if (params.trick.cards().size != 3) return false

        val suitLed = params.trick.cardOf(params.lead)!!.suit // can't be null, there are 3 cards in trick
        val cards = params.trick.cards() + card // let's add our card in question
        val winningCard = cards
            .filter { it.suit == suitLed || it.suit == params.hand.trump.toSuit() }
            .maxBy { cardValue(it, params.hand.trump) }

        return winningCard == card
    }
}

/**
 * If it's a trump suit (not uneufe/obenabe) and not gschobe, lead the first trick with the highest trump card.
 */
object LeadWithHighestTrump : PlayReason {
    override val weight = 10
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        if (!params.notGschobeOpeningLead) return false
        if (!Trump.suits().contains(params.hand.trump)) return false
        val highest = params.playable.filter { it.suit == params.hand.trump.toSuit() }.maxBy { it.rank }
        return card == Card.from(highest)
    }
}

/**
 * If it's uneufe/obenabe and not gschobe, lead the first trick with the best trump card.
 */
object LeadWithHighestTrumpObeabeUneufe : PlayReason {
    override val weight = 10
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        if (!params.notGschobeOpeningLead) return false
        if (!setOf(Trump.OBEABE, Trump.UNEUFE).contains(params.hand.trump)) return false

        val best = when (params.hand.trump) {
            Trump.UNEUFE -> params.playable.minBy { it.rank }
            else -> params.playable.maxBy { it.rank }
        }

        return card == Card.from(best)
    }
}


// In case no other PlayReason matches, you'll get this one
//object PlayFallback : PlayReason {
//    override val weight = 0
//    override fun check(card: Card, params: PlayParams, reasons: List<PlayReason>) = reasons.isEmpty()
//}

// These are applied on by one to each card so the order is essential. We accumulate facts/reasons why each specific
// card should be played and these reasons can depend on each other. The card with the most reason-weight gets played.
val playReasons = listOf(
    LeadWithHighestTrump,
    LeadWithHighestTrumpObeabeUneufe,
    CanWinTrick,
    CanWinTrickWithNoTrump,
    CanWinTrickWithLowest,
//    PlayFallback,
)

fun getPlayCandidate(player: InternalPlayer, state: GameState): PlayCandidate {
    val hand = currentHand(state.hands)
    val trick = currentTrick(state.tricks)
    val cards = cardsInHand(hand, player, state)
    val playable = cards.filter { it.state == CardInHandState.PLAYABLE }
    val lead = currentLeadPositionOfHand(hand, tricksOfHand(state.tricks, hand), state.seats)
    val notGschobeOpeningLead = notGschobeOpeningLead(trick, tricksOfHand(state.tricks, hand), hand)
    val params = PlayParams(hand, trick, playable, lead, state, notGschobeOpeningLead)

    val initialCandidates = playable.map { card -> PlayCandidate(card = Card.from(card), reasons = emptyList()) }
    val candidates = playReasons.fold(initialCandidates) { candidates, reason ->
        candidates.map { candidate ->
            if (reason.check(candidate.card, params, candidates)) {
                candidate.copy(reasons = candidate.reasons + reason)
            } else {
                candidate
            }
        }
    }

    return candidates.maxBy { candidate -> candidate.reasons.sumOf { it.weight } }
}

private fun notGschobeOpeningLead(trick: Trick, tricks: List<Trick>, hand: Hand): Boolean {
    if (hand.gschobe == Gschobe.YES) return false
    if (tricksOfHand(tricks, hand).size > 1) return false
    if (trick.cards().isNotEmpty()) return false

    return true
}