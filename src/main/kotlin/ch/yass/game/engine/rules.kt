package ch.yass.game.engine

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.right
import arrow.core.toOption
import ch.yass.core.error.DomainError.*
import ch.yass.core.helper.logger
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.*


/**
 * Check if the current player was ever dealt and has not yet
 * played * the given card.
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

fun cardIsPlayable(card: Card, player: Player, state: GameState): Either<UnexpectedError, Boolean> {
    either.eager {
        val trick = currentTrick(state.tricks).bind { UnexpectedError("rules.card-is-playable.current-trick.empty") }
        val hand = currentHand(state.hands).bind { UnexpectedError("rules.card-is-playable.current-hand.empty") }
        val seat = playerSeat(player, state.seats).bind { UnexpectedError("rules.card-is-playable.player-seat.empty") }
        val cards = hand.cardsOf(seat.position).filter { playerOwnsCard(player, it, state).bind() }

        // TODO: Take the winner of the last trick, not the starting player of the hand
        val startingPlayerSeat = startingPlayerSeat(
            state.hands,
            state.allPlayers,
            state.seats
        ).bind { UnexpectedError("rules.card-is-playable.starting-player-seat.empty") }


        var playableCards = cards
        var result = true

        // If the starting player didn't play a card yet, there hasn't been a card played at all
        val lead = trick.cardOf(startingPlayerSeat.position)
        if (lead == null) {
            true
        }

        // Something went wrong
        if (hand.trump != Trump.FREESTYLE && hand.trump == null) {
            logger().error("Tried to play card before trump was chosen. Card $card, Player: $player, Trick: $trick")
            false
        }

        // Last card? Feel free to play it
        if (cards.count() == 1) {
            result = true
        }

        // Always follow the suit of the played card - this is our little happy path
        if (lead.suit == card.suit) {
            result = true
        }

        // If it's a "suit" trump, all cards must follow suit except the Trump Jack a.k.a Buur
        if (hand.trump in listOf(Trump.CLUBS, Trump.DIAMONDS, Trump.HEARTS, Trump.SPADES)) {
            // TODO: Also filter out cards that would `undertrump`
            playableCards = cards.filterNot { it.suit != hand.trumpSuit() && it.rank == Rank.JACK }
        }

        // If you still have cards that can follow suit, please do so and don't use the just played card...
        if (playableCards.any { it.suit == lead.suit }) {
            result = false
        }

        true
    }
}

