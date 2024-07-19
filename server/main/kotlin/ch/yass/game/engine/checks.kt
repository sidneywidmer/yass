package ch.yass.game.engine

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import ch.yass.core.error.CardNotPlayable
import ch.yass.core.error.GameError
import ch.yass.core.error.PlayerDoesNotOwnCard
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Seat
import ch.yass.game.dto.db.Trick

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

fun isTrumpSet(hand: Hand?): Boolean = hand?.trump != null

/**
 * Check if the given position already played a weis and if it's the right moment to weis. This is the case if:
 * It's not the welcome hand, it has to be the first trick in the hand, the player has not played a card yet,
 * and the player has the possibility to weis with valid weise.
 */
fun isAlreadyGewiesen(position: Position, hand: Hand, tricks: List<Trick>, weise: List<Weis>): Boolean =
    hand.trump == Trump.FREESTYLE
            || tricks.count() != 1
            || tricks[0].cardOf(position) != null
            || hand.weiseOf(position).isNotEmpty()
            || weise.isEmpty() // No possible weise, we treat this like the player already has gewiesen

fun isAlreadyGschobe(hand: Hand?): Boolean = hand?.gschobe != Gschobe.NOT_YET

fun playerInGame(player: Player, seats: List<Seat>): Boolean = seats.any { it.playerId == player.id }

fun playerOwnsSeat(player: Player, seatUuid: String, seats: List<Seat>): Boolean =
    seats.firstOrNull { it.playerId == player.id }?.uuid.toString() == seatUuid

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

fun isGameFinished(state: GameState): Boolean {
    val settings = state.game.settings
    return when (settings.winningConditionType) {
        WinningConditionType.HANDS -> {
            // We don't need an extra +1 for the welcome hand since completedHands doesn't count it anyway...
            completedHands(state.hands, state.tricks).size >= settings.winningConditionValue
        }

        WinningConditionType.POINTS -> {
            val allComplete = completedHands(state.hands, state.tricks).size == state.hands.size
            (allComplete.takeIf { it }
                ?.let { cardPointsByPositionTotal(state.hands, state.tricks, state.seats).values.sum() }
                ?: 0) == settings.winningConditionValue
        }
    }
}

context(Raise<GameError>)
fun cardIsPlayable(card: Card, player: Player, state: GameState): Boolean {
    ensure(playerOwnsCard(player, card, state)) { PlayerDoesNotOwnCard(player, card, state) }
    ensure(cardFollowsLead(card, player, state)) { CardNotPlayable(card, player, state) }

    return true
}

fun cardFollowsLead(card: Card, player: Player, state: GameState): Boolean {
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
        card.suit == hand.trump?.toSuit() -> true
        else -> false
    }
}

