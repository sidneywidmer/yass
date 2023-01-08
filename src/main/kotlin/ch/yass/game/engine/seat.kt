package ch.yass.game.engine

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.yass.core.error.DomainError.*
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Position
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Seat


fun playerSeat(player: Player, state: GameState): Seat {
    return state.seats.first { it.playerId == player.id }
}

fun freePosition(occupiedSeats: List<Seat>): Either<ValidationError, Position> {
    val occupied = occupiedSeats.map { it.position }.toSet()
    val all = Position.values().toSet()
    val maybePosition = all
        .minus(occupied)
        .randomOrNull()

    return maybePosition?.right() ?: ValidationError("game.take-a-seat.full").left()
}

fun startingPlayerSeat(state: GameState): Seat {
    val hand = currentHand(state)
    val startingPlayer = state.allPlayers.first { it.id == hand.startingPlayerId }

    return state.seats.first { it.playerId == startingPlayer.id }
}
