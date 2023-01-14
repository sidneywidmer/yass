package ch.yass.game.engine

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.yass.core.error.DomainError
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Position
import ch.yass.game.dto.State
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Seat
import ch.yass.game.dto.db.Trick

fun currentTrick(state: GameState): Trick {
    return state.tricks.first()
}

fun currentHand(state: GameState): Hand {
    return state.hands.first()
}

fun playerSeat(player: Player, state: GameState): Seat {
    return state.seats.first { it.playerId == player.id }
}

fun nextState(state: GameState): State {
    val trick = currentTrick(state)

    // Special case for "welcome" trick, only one card is played
    if (trick.cards().count() == 4 && state.hands.count() == 1) {
        return State.NEW_HAND
    }

    if (trick.cards().count() == 4) {
        return State.NEW_TRICK
    }

    if (state.tricks.count() == 10) {
        return State.NEW_HAND
    }

    return State.PLAY_CARD
}

fun nextTrickStartingPlayer(state: GameState): Player {
    val seat = startingPlayerSeat(state)
    val positions = positionsOrderedWithStart(seat.position)
    val nextTrickStartingPosition = positions[1]
    val nextTrickStartingPlayerId = state.seats.first { it.position == nextTrickStartingPosition }.playerId

    return state.allPlayers.first { it.id == nextTrickStartingPlayerId }
}

/**
 * Who's turn is it? Based on the starting player's position we can take the
 * first overall * position that has no card played in the current trick.
 */
fun currentTurnPosition(state: GameState): Position {
    val seat = startingPlayerSeat(state)
    val trick = currentTrick(state)

    val positions = positionsOrderedWithStart(seat.position)

    return positions.first { trick.cardOf(it) == null }
}

/**
 * Get a random free seat.
 */
fun freePosition(occupiedSeats: List<Seat>): Either<DomainError.ValidationError, Position> {
    val occupied = occupiedSeats.map { it.position }.toSet()
    val all = Position.values().toSet()
    val maybePosition = all
        .minus(occupied)
        .randomOrNull()

    return maybePosition?.right() ?: DomainError.ValidationError("game.take-a-seat.full").left()
}

fun startingPlayerSeat(state: GameState): Seat {
    val hand = currentHand(state)
    val startingPlayer = state.allPlayers.first { it.id == hand.startingPlayerId }

    return state.seats.first { it.playerId == startingPlayer.id }
}