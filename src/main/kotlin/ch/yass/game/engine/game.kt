package ch.yass.game.engine

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.*


fun currentTrick(state: GameState): Trick {
    return state.tricks.first()
}

/**
 * Get the order of a round based on a position. So if it's EAST's turn we
 * know the next player is SOUTH. I bet there's a million clever
 * implementations of this but probably none is a simple.
 */
fun positionsOrderedWithStart(position: Position): List<Position> {
    return when (position) {
        Position.NORTH -> listOf(Position.NORTH, Position.EAST, Position.SOUTH, Position.WEST)
        Position.EAST -> listOf(Position.EAST, Position.SOUTH, Position.WEST, Position.NORTH)
        Position.SOUTH -> listOf(Position.SOUTH, Position.WEST, Position.NORTH, Position.EAST)
        Position.WEST -> listOf(Position.WEST, Position.NORTH, Position.EAST, Position.SOUTH)
    }
}

/**
 * Who's turn is it?
 */
fun currentTurnPosition(state: GameState): Position {
    val seat = startingPlayerSeat(state)
    val trick = currentTrick(state)

    val positions = positionsOrderedWithStart(seat.position)

    return positions.first { trick.cardOf(it) !== null }
}

fun playerHasTurn(player: Player, state: GameState): Boolean {
    val activePosition = currentTurnPosition(state)
    val activeSeat = state.seats.first { it.position == activePosition }

    return activeSeat.playerId == player.id
}

fun cardIsPlayable(card: Card, player: Player, state: GameState): Boolean {
    val trick = currentTrick(state)
    val hand = currentHand(state)
    val startingPlayerSeat = startingPlayerSeat(state)
    val seat = playerSeat(player, state)
    val cards = hand.cardsOf(seat.position)

    // If the starting player didn't play a card yet, there hasn't been a card played at all
    val lead = trick.cardOf(startingPlayerSeat.position) ?: return true

    // Last card? Feel free to play it
    if (cards.count() == 1) {
        return true
    }

    // Always follow the suit of the played card - this is our little happy path
    if (lead.suit == card.suit) {
        return true
    }

    //    if (hand.trump in listOf(Trump.CLUBS, Trump.DIAMONDS, Trump.HEARTS, Trump.SPADES))

    // if uneufe/obeabe -> check if player has NO card in hand that matches the suit of the first card
    // if suit trump (unertrumpfen aka you play a trump suit):
    //      - is the first card trump suit? -> true
    //      - has there been another trump played?
    //          - if yes: if your trump is lower -> false, otherwiese -> true
    //          - if no: true
    // if suit trump -> check if player has NO card in hand that matches the suit of the first card - except trump buur


    // If you still have cards that can follow suit, please do so
    if (cards.any { it.suit == lead.suit }) {
        return false
    }

    return true
}

