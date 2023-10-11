package ch.yass.game.engine

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.State
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.*

/**
 * Take all cards the given player was dealt in the current hand and subtract all the
 * cards he already played in the tricks of this hand.
 */
fun unplayedCardsOfPlayer(player: Player, hands: List<Hand>, seats: List<Seat>, tricks: List<Trick>): List<Card> {
    val hand = currentHand(hands)!!
    val seat = playerSeat(player, seats)

    val allCards = hand.cardsOf(seat.position)
    val relevantTricks = tricksOfHand(tricks, hand)
    val playedCards = relevantTricks.mapNotNull { it.cardOf(seat.position) }

    return allCards.minus(playedCards.toSet())
}

fun isTrumpSet(hand: Hand?): Boolean = !(hand?.trump != Trump.FREESTYLE && hand?.trump == null)

fun isAlreadyGschobe(hand: Hand?): Boolean = hand?.gschobe != Gschobe.NOT_YET

/**
 * Check if the current player was ever dealt and has not yet played the given card.
 */
fun playerOwnsCard(player: Player, card: Card, state: GameState): Boolean {
    val unplayedCards = unplayedCardsOfPlayer(player, state.hands, state.seats, state.tricks)

    return unplayedCards.any { it == card }
}

fun expectedState(allowed: List<State>, state: State): Boolean = allowed.contains(state)

fun playerHasActivePosition(player: Player, state: GameState): Boolean {
    val activePlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks) ?: return false

    // Could _in theory_ be null if a game is not full and someone already plays a card
    return activePlayer.id == player.id
}

fun isLastCard(cards: List<Card>) = cards.count() == 1

fun isFollowingLead(lead: Card?, card: Card) = lead?.suit == card.suit

fun couldNotFollowLead(hand: Hand, cards: List<Card>, lead: Card?): Boolean =
    !playableCards(hand, cards).any { it.suit == lead?.suit }

fun isWelcomeHandFinished(trick: Trick, hands: List<Hand>): Boolean = trick.cards().count() == 4 && hands.count() == 1

fun isHandFinished(tricks: List<Trick>): Boolean = tricks.count() == 9 && tricks.first().cards().count() == 4

fun isTrickFinished(trick: Trick): Boolean = trick.cards().count() == 4

/**
 * TODO: Should be a total of points and not a total of hands
 */
fun isGameFinished(hands: List<Hand>, tricks: List<Trick>): Boolean {
    val tricksOfHand = tricksOfHand(tricks, hands.first())
    val lastTrick = tricksOfHand.first()

    return hands.count() == 5 && tricksOfHand.size == 9 && lastTrick.cards().count() == 4
}


fun cardIsPlayable(card: Card, player: Player, state: GameState): Boolean {
    val trick = currentTrick(state.tricks)!!
    val hand = currentHand(state.hands)!!
    val tricks = tricksOfHand(state.tricks, hand)
    val seat = playerSeat(player, state.seats)
    val cards = hand.cardsOf(seat.position).filter { playerOwnsCard(player, it, state) }

    val leadPosition = currentLeadPositionOfHand(hand, tricks, state.seats, state.allPlayers)
    val lead = trick.cardOf(leadPosition)

    return when {
        leadPosition == seat.position -> true  // the player is leading, so let him play
        isLastCard(cards) -> true
        isFollowingLead(lead, card) -> true
        couldNotFollowLead(hand, cards, lead) -> true
        else -> false
    }
}

