package ch.yass.game.engine

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import ch.yass.core.error.CardNotPlayable
import ch.yass.core.error.CardUndertrumps
import ch.yass.core.error.GameError
import ch.yass.core.error.PlayerDoesNotOwnCard
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.InternalPlayer
import ch.yass.game.dto.db.Seat
import ch.yass.game.dto.db.Trick

/**
 * Take all cards the given player was dealt in the current hand and subtract all the
 * cards he already played in the tricks of this hand.
 */
fun unplayedCardsOfPlayer(
    player: InternalPlayer,
    hands: List<Hand>,
    seats: List<Seat>,
    tricks: List<Trick>
): List<Card> {
    val hand = currentHand(hands)
    val seat = playerSeat(player, seats)

    val allCards = hand.cardsOf(seat.position)
    val relevantTricks = tricksOfHand(tricks, hand)
    val playedCards = relevantTricks.mapNotNull { it.cardOf(seat.position) }

    return allCards.minus(playedCards.toSet())
}

fun isTrumpSet(hand: Hand?): Boolean = hand?.trump != Trump.NONE

fun isAlreadyGewiesenSecond(tricks: List<Trick>, hand: Hand): Boolean {
    if (tricks.size != 1) {
        return true
    }

    val currentTrick = currentTrick(tricks)
    if (currentTrick.cards().size != 4) {
        return true
    }

    val remainingWeise = remainingWeise(hand)
    return weisWinner(hand, tricks).flatMap { position -> remainingWeise[position] ?: emptyList() }.isEmpty()
}

/**
 * Check if the given position already played a weis and if it's the right moment to weis. This is the case if:
 * it has to be the first trick in the hand, the player has not played a card yet,
 * and the player has the possibility to weis with valid weise.
 */
fun isAlreadyGewiesen(position: Position, hand: Hand, tricks: List<Trick>, weise: List<Weis>): Boolean =
    tricks.size != 1
            || tricks[0].cardOf(position) != null
            || hand.weiseOf(position).isNotEmpty()
            || weise.isEmpty() // No possible weise, we treat this like the player already has gewiesen

/**
 * The stoeck are weised automatically, this checks if it's time for that: the player holds the stoeck,
 * has not weised them yet and both cards of the pair are played in the current hand.
 */
fun shouldWeisStoeck(hand: Hand, possibleWeise: List<Weis>, position: Position, tricksOfHand: List<Trick>): Boolean {
    // Already weised the stoeck
    if (hand.weiseOf(position).any { w -> w.type == WeisType.STOECK }) {
        return false
    }

    // Not even eligible for stoeck
    val stoeck = possibleWeise.firstOrNull { w -> w.type == WeisType.STOECK }
    if (stoeck == null) {
        return false
    }

    val playedCards = tricksOfHand.mapNotNull { trick -> trick.cardOf(position) }
    return stoeck.cards.all { c -> playedCards.any { it.rank == c.rank && it.suit == c.suit } }
}

fun isAlreadyGschobe(hand: Hand?): Boolean = hand?.gschobe != Gschobe.NOT_YET

fun playerInGame(player: InternalPlayer, seats: List<Seat>): Boolean = seats.any { it.playerId == player.id }

fun playerOwnsSeat(player: InternalPlayer, seatUuid: String, seats: List<Seat>): Boolean =
    seats.firstOrNull { it.playerId == player.id }?.uuid.toString() == seatUuid

/**
 * Check if the current player was ever dealt and has not yet played the given card.
 */
fun playerOwnsCard(player: InternalPlayer, card: Card, state: GameState): Boolean {
    val unplayedCards = unplayedCardsOfPlayer(player, state.hands, state.seats, state.tricks)

    return unplayedCards.any { it == card }
}

fun expectedState(allowed: List<State>, state: State): Boolean = allowed.contains(state)

// activePlayer could _in theory_ be null if a game is not full and someone already plays a card
fun playerHasActivePosition(player: InternalPlayer, state: GameState): Boolean =
    maybeActivePlayer(state.hands, state.allPlayers, state.seats, state.tricks)?.id == player.id

fun isLastCard(cards: List<Card>) = cards.size == 1

fun isFollowingLead(lead: Card?, card: Card) = lead?.suit == card.suit

/**
 * If we have any card with the lead suit than yes, we can follow the lead. BUT
 * if we only have one card that can follow suit and that suit is trump and that
 * card is the jack then no we don't have to.
 */
fun couldFollowLead(cards: List<Card>, lead: Card?, trump: Trump): Boolean {
    val cardsOfLeadSuit = cards.filter { it.suit == lead?.suit }
    if (lead?.suit == trump.toSuit() && cardsOfLeadSuit.size == 1) {
        return cardsOfLeadSuit.first().rank != Rank.JACK
    }
    return cardsOfLeadSuit.isNotEmpty()
}

fun isHandFinished(tricks: List<Trick>): Boolean = tricks.size == 9 && tricks.first().cards().size == 4

fun isTrickFinished(trick: Trick): Boolean = trick.cards().size == 4

fun isFirstRegularTrickOfHand(tricks: List<Trick>): Boolean = tricks.size == 1

fun isGameFinished(state: GameState): Boolean {
    val settings = state.game.settings
    return when (settings.winningConditionType) {
        WinningConditionType.HANDS -> {
            completedHands(state.hands, state.tricks).size >= settings.winningConditionValue
        }

        WinningConditionType.POINTS -> {
            val points = pointsByPositionTotal(state.hands, state.tricks)
            Team.entries.any { team ->
                team.positions.sumOf {
                    points.getValue(it).total()
                } >= settings.winningConditionValue
            }
        }
    }
}

context(r: Raise<GameError>)
fun cardIsPlayable(card: Card, player: InternalPlayer, state: GameState): Boolean {
    r.ensure(playerOwnsCard(player, card, state)) { PlayerDoesNotOwnCard(player, card, state) }
    r.ensure(cardFollowsLead(card, player, state)) { CardNotPlayable(card, player, state) }
    r.ensure(!cardUndertrumps(card, player, state)) { CardUndertrumps(card, player, state) }

    return true
}

fun cardUndertrumps(card: Card, player: InternalPlayer, state: GameState): Boolean {
    val hand = currentHand(state.hands)
    if (hand.trump.toSuit() != card.suit) {
        return false // card is not trump suit, can't undertrump :)
    }

    val seat = playerSeat(player, state.seats)
    val cards = hand.cardsOf(seat.position).filter { playerOwnsCard(player, it, state) }
    val unplayedTrumps = cards.filter { it.suit == hand.trump.toSuit() }
    if (unplayedTrumps.size == cards.size) {
        return false // player has only trumps left, impossible to undertrump
    }

    val trick = currentTrick(state.tricks)
    val leadPosition = currentLeadPositionOfHand(hand, tricksOfHand(state.tricks, hand), state.seats)
    val leadCard = trick.cardOf(leadPosition)
    if (hand.trump.toSuit() == leadCard?.suit) {
        return false // first card in trick is a trump, it's impossible to undertrump in this whole trick
    }

    val highestTrumpInTrick = trick.cards()
        .filter { it.suit == hand.trump.toSuit() }
        .filter { it != leadCard }
        .maxByOrNull { cardValue(it, hand.trump) }

    if (highestTrumpInTrick == null) {
        return false // no other card in the trick (except maybe lead but doesn't matter) is a trump
    }

    if (cardValue(highestTrumpInTrick, hand.trump) < cardValue(card, hand.trump)) {
        return false // we're not undertrumping, the card is higher
    }

    return true // Undertrump!
}

fun cardFollowsLead(card: Card, player: InternalPlayer, state: GameState): Boolean {
    val trick = currentTrick(state.tricks)
    val hand = currentHand(state.hands)
    val tricks = tricksOfHand(state.tricks, hand)
    val seat = playerSeat(player, state.seats)
    val cards = hand.cardsOf(seat.position).filter { playerOwnsCard(player, it, state) }

    val leadPosition = currentLeadPositionOfHand(hand, tricks, state.seats)
    val lead = trick.cardOf(leadPosition)

    return when {
        leadPosition == seat.position -> true  // the player is leading, so let him play
        isLastCard(cards) -> true
        isFollowingLead(lead, card) -> true
        !couldFollowLead(cards, lead, hand.trump) -> true
        card.suit == hand.trump.toSuit() -> true
        else -> false
    }
}

