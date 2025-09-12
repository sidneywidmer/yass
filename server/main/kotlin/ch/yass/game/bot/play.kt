package ch.yass.game.bot

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.CardInHand
import ch.yass.game.dto.CardInHandState
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Suit
import ch.yass.game.dto.Team
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.InternalPlayer
import ch.yass.game.dto.db.Seat
import ch.yass.game.dto.db.Trick
import ch.yass.game.engine.*

// https://www.watson.ch/schweiz/sport/293024792-jass-tipps-welche-karte-wuerdest-du-hier-spielen
// https://www.swisslos.ch/de/jass/informationen/tipps-vom-jass-onkel/schieber-jass.html
// https://s1403f1212affd0cf.jimcontent.com/download/version/1653654748/module/8968421285/name/Tipps%20Schieber1910.pdf
// https://www.earthli.com/jass/manual.php?page=strategies


data class PlayCandidate(val card: Card, val reasons: List<PlayReason>)
data class OutOfSuit(val position: Position, val suits: Set<Suit>)

data class PlayParams(
    val hand: Hand,
    val trick: Trick,
    val playable: List<CardInHand>,
    val lead: Position,
    val state: GameState,
    val notGschobeOpeningLead: Boolean, // First card in the first trick of an ungschobe hand
    val outOfSuit: List<OutOfSuit>,
    val bock: List<Card>,
    val remainingTrumps: List<Card>
//    val points: Int,
//    val partnerWinning: Boolean,
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
    override val weight = 4
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
        val highest = params.playable
            .filter { it.suit == params.hand.trump.toSuit() }
            .maxBy { cardValue(Card.from(it), params.hand.trump) }
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

        val best = params.playable.maxBy { cardValue(Card.from(it), params.hand.trump) }

        return card == Card.from(best)
    }
}

/**
 * When not in the opening trick of the hand, if we detected that all players after us can't beat
 * given card we can play it.
 */
object PlayBock : PlayReason {
    override val weight = 10
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        if (tricksOfHand(params.state.tricks, params.hand).size == 1) return false
        return params.bock.contains(card) && params.remainingTrumps.isEmpty()
    }
}

object Schmiere : PlayReason {
    override val weight = 10
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        if (tricksOfHand(params.state.tricks, params.hand).size == 1) return false
        if (cardValue(card, params.hand.trump) < 8) return false

        return false
//        return partnerGuaranteedWin
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
    PlayBock,
    Schmiere
//    PlayFallback,
)

fun getPlayCandidate(player: InternalPlayer, state: GameState): PlayCandidate {
    val hand = currentHand(state.hands)
    val trick = currentTrick(state.tricks)
    val cards = cardsInHand(hand, player, state)
    val playable = cards.filter { it.state == CardInHandState.PLAYABLE }
    val lead = currentLeadPositionOfHand(hand, tricksOfHand(state.tricks, hand), state.seats)
    val notGschobeOpeningLead = notGschobeOpeningLead(trick, tricksOfHand(state.tricks, hand), hand)
    val outOfSuit = outOfSuit(hand, tricksOfHand(state.tricks, hand), state.seats)
    val bock = bock(tricksOfHand(state.tricks, hand), hand.trump)
    val remainingTrumps = remainingTrumps(tricksOfHand(state.tricks, hand), hand.trump)
    val isPartnerWinning = isPartnerWinning()
    val params = PlayParams(hand, trick, playable, lead, state, notGschobeOpeningLead, outOfSuit, bock, remainingTrumps)

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

private fun isPartnerWinning(position: Position, trick: Trick, lead: Position): Boolean {
    val partner = Team.getPartner(position)
    val partnerCard = trick.cardOf(partner) ?: return false
    hier gehts weiter
    // the partners card is currently winning, it's bock and no more trumps
    TODO("Not yet implemented")
}

private fun remainingTrumps(tricks: List<Trick>, trump: Trump): List<Card> {
    val all = deck()
        .map { Card(it.second, it.first, "french") }
        .filter { trump.equalsSuit(it.suit) }
    val played = tricks.flatMap { it.cards() }.filter { trump.equalsSuit(it.suit) }

    return all.minus(played.toSet())
}

/**
 * Get bock cards from all suits (including trump)
 */
private fun bock(tricks: List<Trick>, trump: Trump): List<Card> {
    val all = deck().map { Card(it.second, it.first, "french") }
    val played = tricks.flatMap { it.cards() }

    return all.minus(played.toSet())
        .groupBy { it.suit }
        .map { (_, cards) -> cards.maxBy { cardValue(it, trump) } }
}

/**
 * By looking at all past tricks we figure out who could not follow suit for sure. If they played
 * a trump instead of following suit we can't really be sure.
 */
private fun outOfSuit(hand: Hand, tricks: List<Trick>, seats: List<Seat>): List<OutOfSuit> {
    return hand.tricksWithPoints(tricks.filter { it.cards().size == 4 })
        .flatMap { trickWithPoints ->
            val leadSuit = trickWithPoints.trick.cardOf(trickWithPoints.lead)!!.suit
            trickWithPoints.trick.cardsByPosition()
                .filter { it.suit != leadSuit }
                .filter { !hand.trump.equalsSuit(it.suit) }
                .map { it.position }
                .map { OutOfSuit(it, setOf(leadSuit)) }
        }
        .groupBy { it.position }
        .mapValues { (_, outOfSuits) -> outOfSuits.flatMap { it.suits }.toSet() }
        .map { OutOfSuit(it.key, it.value) }
}

private fun notGschobeOpeningLead(trick: Trick, tricks: List<Trick>, hand: Hand): Boolean {
    if (hand.gschobe == Gschobe.YES) return false
    if (tricksOfHand(tricks, hand).size > 1) return false
    if (trick.cards().isNotEmpty()) return false

    return true
}