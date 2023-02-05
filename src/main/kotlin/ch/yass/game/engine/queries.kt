package ch.yass.game.engine

import arrow.core.*
import arrow.core.continuations.either
import arrow.core.continuations.option
import ch.yass.core.error.DomainError.*
import ch.yass.core.helper.logger
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
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

fun tricksOfHand(tricks: List<Trick>, hand: Hand): List<Trick> {
    return tricks.filter { it.handId == hand.id }
}

/**
 * Get the player sitting at the given position.
 */
fun playerAtPosition(position: Position, seats: List<Seat>, players: List<Player>): Option<Player> {
    val seat = seats.firstOrNull { it.position == position }.toOption()
    return seat.mapNotNull { players.firstOrNull { seat -> seat.id == it.playerId } }
}

fun nextState(state: GameState): State {
    val trick = currentTrick(state.tricks).getOrElse { null }
    val hand = currentHand(state.hands)
    val player = currentTurnPlayer(state.hands, state.allPlayers, state.seats, state.tricks).getOrElse { null }
    val tricks = hand.map { tricksOfHand(state.tricks, it) }
        .tapNone { logger().error("Defaulting to empty list for tricks, State: $state.") }
        .getOrElse { emptyList() }

    return when {
        trick == null -> State.NEW_TRICK
        player == null -> {
            logger().error("Could not find player for state $state. Praying and falling back to PLAY_CARD.")
            State.PLAY_CARD
        }
        // Special case for "welcome" trick, only one card is played per player
        isWelcomeHandFinished(trick, state.hands) -> State.NEW_HAND
        isHandFinished(tricks) -> State.NEW_HAND
        isTrickFinished(trick) -> State.NEW_TRICK
        isGameFinished(state.hands) -> State.FINISHED
        isTrumpSet(hand) -> if (player.bot) State.TRUMP_BOT else State.TRUMP
        else -> if (player.bot) State.PLAY_CARD_BOT else State.PLAY_CARD
    }
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
 * first overall position that has no card played in the current trick.
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

fun currentTurnPlayer(
    hands: List<Hand>,
    players: List<Player>,
    seats: List<Seat>,
    tricks: List<Trick>
): Option<Player> =
    currentTurnPosition(hands, players, seats, tricks).flatMap { playerAtPosition(it, seats, players) }

/**
 * Get a random free seat.
 */
fun freePosition(occupiedSeats: List<Seat>): Either<ValidationError, Position> {
    val occupied = occupiedSeats.map { it.position }.toSet()
    val all = Position.values().toSet()
    val maybePosition = all.minus(occupied).randomOrNull()

    return maybePosition?.right() ?: ValidationError("game.take-a-seat.full").left()
}

/**
 * Find at which seat the starting player of the current hand sits.
 */
fun startingPlayerSeat(hands: List<Hand>, players: List<Player>, seats: List<Seat>): Option<Seat> = option.eager {
    val hand = currentHand(hands).bind()
    val startingPlayer = players.firstOrNull { it.id == hand.startingPlayerId }.toOption().bind()

    seats.firstOrNull { it.playerId == startingPlayer.id }.toOption().bind()
}

/**
 * If it's a "suit" trump, all cards must follow suit except the Trump Jack a.k.a Buur.
 * TODO: How to handle undertrump?
 */
fun playableCards(hand: Hand, cards: List<Card>): List<Card> =
    when (hand.trump) {
        in listOf(
            Trump.CLUBS,
            Trump.DIAMONDS,
            Trump.HEARTS,
            Trump.SPADES
        ) -> cards.filterNot { it.suit != hand.trumpSuit() && it.rank == Rank.JACK }

        else -> cards
    }