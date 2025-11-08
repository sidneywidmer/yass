package ch.yass.game.bot

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.InternalPlayer
import ch.yass.game.dto.db.Trick
import ch.yass.game.engine.*

// https://www.watson.ch/schweiz/sport/293024792-jass-tipps-welche-karte-wuerdest-du-hier-spielen
// https://www.swisslos.ch/de/jass/informationen/tipps-vom-jass-onkel/schieber-jass.html
// https://s1403f1212affd0cf.jimcontent.com/download/version/1653654748/module/8968421285/name/Tipps%20Schieber1910.pdf
// https://www.earthli.com/jass/manual.php?page=strategies

data class PlayCandidate(val card: Card, val reasons: List<PlayReason>)
data class OutOfSuit(val position: Position, val suits: Set<Suit>)

data class PlayParams(
    val position: Position,
    val hand: Hand,
    val trick: Trick,
    val playable: List<Card>,
    val lead: Position,
    val state: GameState, // First card in the first trick of an ungschobe hand
    val notGschobeOpeningLead: Boolean,
    val outOfSuit: List<OutOfSuit>,
    val bock: List<Card>,
    val remainingTrumps: List<Card>,
    val isPartnerWinning: Boolean
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
object CanWinTrickWithLastCardLowest : PlayReason {
    override val weight = 2
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        // If we can't win the trick with given card return
        if (!candidates.reasonsOf(card).contains(CanWinTrickWithLastCard)) return false

        // Get the lowest card that can win the trick of the same suit as given card
        val lowestOfSuit = candidates
            .filter { it.reasons.contains(CanWinTrickWithLastCard) }
            .filter { it.card.suit == card.suit }
            .map { it.card }
            .minBy { it.rank }

        return lowestOfSuit == card
    }
}

/**
 * Check if we can win the trick without using a trump. Only relevant for last card.
 */
object CanWinTrickWithLastCardNoTrump : PlayReason {
    override val weight = 4
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        // If we can't win the trick with given card return
        if (!candidates.reasonsOf(card).contains(CanWinTrickWithLastCard)) return false
        if (card.suit != params.hand.trump.toSuit()) return true
        return false
    }
}

/**
 * If this is the last card played, check if we have an easy way to win the trick.
 */
object CanWinTrickWithLastCard : PlayReason {
    override val weight = 6
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        if (params.trick.cards().size != 3) return false

        val simulatedTrick = params.trick.withCard(params.position, card)
        val winner = winningPositionOfTrick(simulatedTrick, params.lead, params.hand.trump)

        return simulatedTrick.cardOf(winner) == card
    }
}

/**
 * Can for sure win trick with this non bock card.
 */
object CanWinTrickWithNonBock : PlayReason {
    override val weight = 4
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        if (!candidates.reasonsOf(card).contains(CanWinTrickPotentiallyWithNonBock)) return false
        val opponentsOutOfTrumpSuit = params.outOfSuit
            .filter { Team.getOpponents(params.position).contains(it.position) }
            .filter { it.suits.contains(params.hand.trump.toSuit()) }

        // I lead the trick
        if (params.trick.cards().isEmpty()) {
            return opponentsOutOfTrumpSuit.size == 2
        }

        val upcoming = upcomingPositions(params.lead, params.position)
        return opponentsOutOfTrumpSuit.singleOrNull()?.position in upcoming
    }
}

/**
 * Ignoring trumps, if play this card the opponents can not farben (but still may have trumps).
 */
object CanWinTrickPotentiallyWithNonBock : PlayReason {
    override val weight = 4
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        if (params.bock.contains(card)) return false
        val opponentsOutOfSuit = params.outOfSuit
            .filter { Team.getOpponents(params.position).contains(it.position) }
            .filter { it.suits.contains(card.suit) }

        // I lead the trick
        if (params.trick.cards().isEmpty()) {
            return opponentsOutOfSuit.size == 2
        }

        val upcoming = upcomingPositions(params.lead, params.position)
        return opponentsOutOfSuit.singleOrNull()?.position in upcoming
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
            .maxBy { cardValue(it, params.hand.trump) }

        return card == highest
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

        val best = params.playable.maxBy { cardValue(it, params.hand.trump) }

        return card == best
    }
}

/**
 * If we have a bock for now just play it unless it's a trump and our opponents are out of trumps or
 * we have anything better.
 */
object PlayBock : PlayReason {
    override val weight = 5
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        // Don't boost this card with bock weight if not necessary
        val betterChoices = setOf(CanWinTrickWithLastCard, CanWinTrickWithNonBock)
        if (candidates.reasonsOf(card).any { it in betterChoices }) return false

        // If card is trump and the opponent is out of trump we don't have to
        val isTrump = params.hand.trump.equalsSuit(card.suit)
        val opponentsOutOfTrumpSuit = params.outOfSuit
            .filter { Team.getOpponents(params.position).contains(it.position) }
            .filter { it.suits.contains(params.hand.trump.toSuit()) }

        if (isTrump && opponentsOutOfTrumpSuit.size == 2) return false

        return isBock(
            card,
            params.playable,
            params.bock,
            params.remainingTrumps,
            params.hand.trump,
            params.trick,
            params.position,
            params.lead
        )
    }
}

/**
 * If we're leading or cant farbe (and have not identified anything better to play) signal a strong
 * suit with a low card from that suit. Strong suit meaning we have near bock aka a card 1 bellow
 * current bock in that suit.
 */
object SignalStrongSuitNearBock : PlayReason {
    override val weight = 5
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        if (candidates.reasonsOf(card).isNotEmpty()) return false
        if (cardPoints(card, params.hand.trump) >= 1) return false // only use braettli for that

        val leadCard = params.trick.cardOf(params.lead)
        val canFarbe = params.playable.any { it.suit == leadCard?.suit }
        if (params.trick.cards().isNotEmpty() || !canFarbe) return false

        // Find cards that are soon bock, with just one card higher than them currently being bock.
        // If we find some of them play a low card from that suit
        val bockOfSuit = params.bock.firstOrNull { it.suit == card.suit }
        if (bockOfSuit == null) return false

        val allOfSuit = allOfSuit(card.suit).map { Card(it.second, it.first) }
        val allRemainingOfSuit = sortByPoints(allOfSuit, params.hand.trump).dropWhile { it != bockOfSuit }
        val nearBock = allRemainingOfSuit[1] // second card in that list is our near bock

        // Unlucky, we don't have this near bock
        if (!params.playable.contains(nearBock)) return false

        // If this card is the nearBock, definitely don't play it
        if (card == nearBock) return false

        // If this card is the lowest in this suit, play it
        return card == sortByPoints(params.playable.filter { it.suit == card.suit }, params.hand.trump).last()
    }
}

/**
 * Play something small if partner is winning anyway. Check `Schmiere` which takes precedence.
 */
object PartnerIsWinning : PlayReason {
    override val weight = 5
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        if (tricksOfHand(params.state.tricks, params.hand).size <= 1) return false
        if (cardPoints(card, params.hand.trump) > 1) return false

        return params.isPartnerWinning
    }
}

object Schmiere : PlayReason {
    override val weight = 10
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        if (tricksOfHand(params.state.tricks, params.hand).size <= 1) return false
        if (cardPoints(card, params.hand.trump) < 8) return false
        if (card.rank == Rank.ACE) return false

        return params.isPartnerWinning
    }
}

object JustPlayLow : PlayReason {
    override val weight = 2
    override fun check(card: Card, params: PlayParams, candidates: List<PlayCandidate>): Boolean {
        if (candidates.reasonsOf(card).isNotEmpty()) return false
        val lowestPlayable = sortByPoints(params.playable, params.hand.trump)

        return lowestPlayable.last() == card
    }
}


// These are applied on by one to each card so the order is essential. We accumulate facts/reasons why each specific
// card should be played and these reasons can depend on each other. The card with the most reason-weight gets played.
val playReasons = listOf(
    LeadWithHighestTrump,
    LeadWithHighestTrumpObeabeUneufe,
    CanWinTrickWithLastCard,
    CanWinTrickWithLastCardNoTrump,
    CanWinTrickWithLastCardLowest,
    CanWinTrickPotentiallyWithNonBock,
    CanWinTrickWithNonBock,
    PlayBock,
    Schmiere,
    PartnerIsWinning,
    SignalStrongSuitNearBock,
    JustPlayLow
)

fun getPlayCandidate(player: InternalPlayer, state: GameState): PlayCandidate {
    // Welcome hand - let's keep it simple here
    val hand = currentHand(state.hands)
    val trick = currentTrick(state.tricks)
    val cards = cardsInHand(hand, player, state)
    val playable = cards.filter { it.state == CardInHandState.PLAYABLE }.map { Card.from(it) }
    val lead = currentLeadPositionOfHand(hand, tricksOfHand(state.tricks, hand), state.seats)
    val position = playerSeat(player, state.seats).position

    if (state.hands.size == 1) {
        return PlayCandidate(Card.from(cards.random()), emptyList())
    }

    // Some "Facts" about the current state of the game
    val notGschobeOpeningLead = notGschobeOpeningLead(trick, tricksOfHand(state.tricks, hand), hand)
    val outOfSuit = outOfSuit(hand, tricksOfHand(state.tricks, hand))
    val bock = remainingBocks(tricksOfHand(state.tricks, hand), hand.trump)
    val remainingTrumps = remainingTrumps(tricksOfHand(state.tricks, hand), hand.trump)
    val isPartnerWinning = isPartnerWinning(position, trick, lead, hand.trump, playable, bock, remainingTrumps)
    val params = PlayParams(
        position,
        hand,
        trick,
        playable,
        lead,
        state,
        notGschobeOpeningLead,
        outOfSuit,
        bock,
        remainingTrumps,
        isPartnerWinning
    )

    val initialCandidates = playable.map { card -> PlayCandidate(card = card, reasons = emptyList()) }
    val candidates = playReasons.fold(initialCandidates) { candidates, reason ->
        candidates.map { candidate ->
            if (reason.check(candidate.card, params, candidates)) {
                candidate.copy(reasons = candidate.reasons + reason)
            } else {
                candidate
            }
        }
    }

    // if no good card to play -> play a low value one
    val final = candidates.maxBy { candidate -> candidate.reasons.sumOf { it.weight } }
//    logger().info(
//        "{} played {} because: {}",
//        position.name,
//        final.card,
//        final.reasons.joinToString { it.javaClass.simpleName })

    return final
}

private fun isPartnerWinning(
    position: Position,
    trick: Trick,
    lead: Position,
    trump: Trump,
    playable: List<Card>,
    bock: List<Card>,
    remainingTrumps: List<Card>
): Boolean {
    // If my partner didn't play a card yet, we can skip this one
    val partner = Team.getPartner(position)
    val partnerCard = trick.cardOf(partner) ?: return false

    // All cards played, just check if they're currently winning since just my card will be played
    if (trick.cards().size == 3) {
        return winningPositionOfTrick(trick, lead, trump) == partner
    }

    // There's still one opponent after me, so we need to check if the card played by my partner is bock
    return isBock(partnerCard, playable, bock, remainingTrumps, trump, trick, position, lead)
}

private fun remainingTrumps(tricks: List<Trick>, trump: Trump): List<Card> {
    val all = deck()
        .map { Card(it.second, it.first) }
        .filter { trump.equalsSuit(it.suit) }
    val played = tricks.flatMap { it.cards() }.filter { trump.equalsSuit(it.suit) }

    return all.minus(played.toSet())
}

/**
 * Get bock cards from all suits (including trump)
 */
private fun remainingBocks(tricks: List<Trick>, trump: Trump): List<Card> {
    val all = deck().map { Card(it.second, it.first) }
    val played = tricks.flatMap { it.cards() }

    return all.minus(played.toSet())
        .groupBy { it.suit }
        .map { (_, cards) -> cards.maxBy { cardValue(it, trump) } }
}

/**
 * By looking at all past tricks we figure out who could not follow suit for sure. If they played
 * a trump instead of following suit we can't really be sure.
 */
private fun outOfSuit(hand: Hand, tricks: List<Trick>): List<OutOfSuit> {
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

/**
 * Check if given card is currently bock (including trump bock) AND beats all already played cards in trick.
 */
private fun isBock(
    card: Card,
    playable: List<Card>,
    bock: List<Card>,
    remainingTrumps: List<Card>,
    trump: Trump,
    trick: Trick,
    position: Position,
    lead: Position,
): Boolean {
    val notOwnedTrumps = remainingTrumps.filter { !playable.contains(it) }
    val suitBock = bock.contains(card) && notOwnedTrumps.isEmpty()
    val trumpBock = bock.contains(card) && trump.equalsSuit(card.suit)

    val simulatedTrick = trick.withCard(position, card)
    val winner = winningPositionOfTrick(simulatedTrick, lead, trump)

    return winner == position && (suitBock || trumpBock)
}