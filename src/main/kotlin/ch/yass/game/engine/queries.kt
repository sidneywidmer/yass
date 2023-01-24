package ch.yass.game.engine

import arrow.core.*
import arrow.core.continuations.option
import ch.yass.core.error.DomainError
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.Position
import ch.yass.game.dto.State
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Seat
import ch.yass.game.dto.db.Trick

fun currentTrick(tricks: List<Trick>): Option<Trick> {
    return tricks.firstOrNull().toOption()
}

fun currentHand(hands: List<Hand>): Option<Hand> {
    return hands.firstOrNull().toOption()
}

fun playerSeat(player: Player, seats: List<Seat>): Option<Seat> {
    return seats.firstOrNull { it.playerId == player.id }.toOption()
}

/**
 * Get the player sitting at the given position.
 */
fun playerAtPosition(position: Position, seats: List<Seat>, players: List<Player>): Option<Player> {
    val seat = seats.firstOrNull { it.position == position }.toOption()
    return seat.mapNotNull { players.firstOrNull { seat -> seat.id == it.playerId } }
}

fun nextState(state: GameState): Option<State> = option.eager {
    val trick = currentTrick(state.tricks).bind()
    var nextState: State = State.PLAY_CARD

    // Special case for "welcome" trick, only one card is played
    if (trick.cards().count() == 4 && state.hands.count() == 1) {
        nextState = State.NEW_HAND
    }

    if (trick.cards().count() == 4) {
        nextState = State.NEW_TRICK
    }

    if (state.tricks.count() == 9) {
        nextState = State.NEW_HAND
    }

    nextState
}

/**
 * What's the last card that was played by given player?
 */
fun lastCardOfPlayer(player: Player, tricks: List<Trick>, seats: List<Seat>): Option<Card> = option.eager {
    val trick = currentTrick(tricks).bind()
    val seat = playerSeat(player, seats).bind()

    trick.cardOf(seat.position).toOption().bind()
}

/**
 * Find the player who starts the next trick.
 */
fun nextTrickStartingPlayer(hands: List<Hand>, players: List<Player>, seats: List<Seat>): Option<Player> =
    option.eager {
        val seat = startingPlayerSeat(hands, players, seats).bind()
        val nextTrickStartingPosition = positionsOrderedWithStart(seat.position)[1]
        val startingSeat = seats.firstOrNull { it.position == nextTrickStartingPosition }.toOption().bind()

        players.firstOrNull { it.id == startingSeat.playerId }.toOption().bind()
    }

/**
 * Who's turn is it? Based on the starting player's position we can take the
 * first overall * position that has no card played in the current trick.
 */
fun currentTurnPosition(
    hands: List<Hand>,
    players: List<Player>,
    seats: List<Seat>,
    tricks: List<Trick>
): Option<Position> = option.eager {
    val seat = startingPlayerSeat(hands, players, seats).bind()
    val trick = currentTrick(tricks).bind()

    val positions = positionsOrderedWithStart(seat.position)

    positions.firstOrNull { trick.cardOf(it) == null }.toOption().bind()
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

/**
 * Find at which seat the player of the current hand sits.
 */
fun startingPlayerSeat(hands: List<Hand>, players: List<Player>, seats: List<Seat>): Option<Seat> = option.eager {
    val hand = currentHand(hands).bind()
    val startingPlayer = players.firstOrNull { it.id == hand.startingPlayerId }.toOption().bind()

    seats.firstOrNull { it.playerId == startingPlayer.id }.toOption().bind()
}