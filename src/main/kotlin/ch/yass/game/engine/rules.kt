package ch.yass.game.engine

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.right
import arrow.core.toOption
import ch.yass.core.error.DomainError
import ch.yass.core.error.DomainError.*
import ch.yass.core.helper.logger
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.*


/**
 * Check if the current player was ever dealt and has not yet
 * played the given card.
 */
fun playerOwnsCard(player: Player, card: Card, state: GameState): Either<UnexpectedError, Boolean> = either.eager {
    val hand = currentHand(state.hands).bind { UnexpectedError("rules.player-owns-card.current-hand.empty") }
    val seat = playerSeat(player, state.seats).bind { UnexpectedError("rules.player-owns-card.seat.empty") }

    val allCards = hand.cardsOf(seat.position)

    val tricks = state.tricks.filter { it.handId == hand.id }
    val playedCards = tricks.map { it.cardOf(seat.position) }
    val unplayedCards = allCards.minus(playedCards.toSet())

    unplayedCards.any { it == card }.right().bind()
}

fun playerHasTurn(player: Player, state: GameState): Either<UnexpectedError, Boolean> = either.eager {
    val activePosition = currentTurnPosition(
        state.hands,
        state.allPlayers,
        state.seats,
        state.tricks
    ).bind { UnexpectedError("rules.player-has-turn.active-position.empty") }
    val activeSeat = state.seats.firstOrNull { it.position == activePosition }.toOption()

    // Could _in theory_ be null if a game is not full and someone already plays a card
    activeSeat.fold({ false }, { it.playerId == player.id })
}

fun isLeadPlayed(trick: Trick, seat: Seat): Boolean = trick.cardOf(seat.position) != null

fun isLastCard(cards: List<Card>) = cards.count() == 1

fun isFollowingLead(trick: Trick, seat: Seat, card: Card) = trick.cardOf(seat.position)?.suit == card.suit

fun couldNotFollowLead(hand: Hand, cards: List<Card>, lead: Card?): Boolean =
    !playableCards(hand, cards).any { it.suit == lead?.suit }

fun cardIsPlayable(card: Card, player: Player, state: GameState): Either<UnexpectedError, Boolean> = either.eager {
    val trick = currentTrick(state.tricks).bind { UnexpectedError("rules.card-is-playable.current-trick.empty") }
    val hand = currentHand(state.hands).bind { UnexpectedError("rules.card-is-playable.current-hand.empty") }
    val seat = playerSeat(player, state.seats).bind { UnexpectedError("rules.card-is-playable.player-seat.empty") }
    val cards = hand.cardsOf(seat.position).filter { playerOwnsCard(player, it, state).bind() }

    // TODO: Take the winner of the last trick, not the starting player of the hand
    val startingPlayerSeat = startingPlayerSeat(
        state.hands,
        state.allPlayers,
        state.seats,
    ).bind { UnexpectedError("rules.card-is-playable.starting-player-seat.empty") }
    val lead = trick.cardOf(startingPlayerSeat.position)

    if (hand.trump != Trump.FREESTYLE && hand.trump == null) {
        logger().error("Tried to play card before trump was chosen. Card $card, Player: $player, Trick: $trick")
        shift<UnexpectedError>(UnexpectedError("rules.card-is-playable.trump.invalid"))
    }

    when {
        !isLeadPlayed(trick, startingPlayerSeat) -> true
        isLastCard(cards) -> true
        isFollowingLead(trick, startingPlayerSeat, card) -> true
        couldNotFollowLead(hand, cards, lead) -> true
        else -> false
    }
}

