package ch.yass.game.engine

import arrow.core.raise.Raise
import ch.yass.core.error.GameAlreadyFull
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Seat
import ch.yass.game.dto.db.Trick
import kotlin.collections.contains

fun currentTrick(tricks: List<Trick>): Trick? = tricks.firstOrNull()

fun currentHand(hands: List<Hand>): Hand? = hands.firstOrNull()

fun playerSeat(player: Player, seats: List<Seat>): Seat =
    seats.first { it.playerId == player.id }

fun tricksOfHand(tricks: List<Trick>, hand: Hand): List<Trick> {
    return tricks.filter { it.handId == hand.id }
}

/**
 * Get the player sitting at the given position.
 */
fun playerAtPosition(position: Position, seats: List<Seat>, players: List<Player>): Player? {
    return seats.firstOrNull { it.position == position }
        .let { players.firstOrNull { seat -> seat.id == it?.playerId } }
}

/**
 * If we don't have a winner of the last trick, it means this is the first trick in the current hand. If this is
 * the case the starting player of the current hands card is the lead.
 */
fun currentLeadPosition(state: GameState): Position {
    val hand = currentHand(state.hands)!!
    val lastTricksWinnerPosition = winningPositionOfLastTrick(hand, state.tricks, state.seats)
    if (lastTricksWinnerPosition != null) {
        return lastTricksWinnerPosition
    }

    val startingPlayer = startingPlayersSeatOfCurrentHand(state.hands, state.allPlayers, state.seats)
    return startingPlayer.position
}

fun nextState(state: GameState): State {
    val trick = currentTrick(state.tricks)
    val hand = currentHand(state.hands)
    val player = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)!!
    val tricks = hand?.let { tricksOfHand(state.tricks, it) } ?: emptyList()

    return when {
        trick == null -> State.NEW_TRICK
        isGameFinished(state.hands, state.tricks) -> State.FINISHED

        // Special case for "welcome" trick, only one card is played per player
        isWelcomeHandFinished(trick, state.hands) -> State.NEW_HAND

        isHandFinished(tricks) -> State.NEW_HAND
        isTrickFinished(trick) -> State.NEW_TRICK
        !isTrumpSet(hand) -> if (player.bot) State.TRUMP_BOT else State.TRUMP
        else -> if (player.bot) State.PLAY_CARD_BOT else State.PLAY_CARD
    }
}

/**
 * Find the player who starts the next trick.
 */
fun nextHandStartingPlayer(hands: List<Hand>, players: List<Player>, seats: List<Seat>): Player {
    val seat = startingPlayersSeatOfCurrentHand(hands, players, seats)
    val nextTrickStartingPosition = positionsOrderedWithStart(seat.position)[1]
    val startingSeat = seats.first { it.position == nextTrickStartingPosition }

    return players.first { it.id == startingSeat.playerId }
}

/**
 * Which player is expected to take the next action? If there are no tricks played yet it's the
 * starting player of the hand. If the current trick is "complete", aka 4 cards have been
 * played, it's the winner of the last trick. If not, it's the neighbor of the player
 * who played a card last.
 */
fun activePosition(
    hands: List<Hand>,
    players: List<Player>,
    seats: List<Seat>,
    tricks: List<Trick>
): Position? {
    val trick = currentTrick(tricks)
    val hand = currentHand(hands)!!
    return when {
        trick == null -> startingPlayersSeatOfCurrentHand(hands, players, seats).position
        trick.cards().size < 4 -> {
            val seat = startingPlayersSeatOfCurrentHand(hands, players, seats)
            val positions = positionsOrderedWithStart(seat.position)

            positions.first { trick.cardOf(it) == null }
        }

        else -> winningPositionOfCurrentTrick(hand, tricksOfHand(tricks, hand), seats)
    }
}

fun winningPositionOfCurrentTrick(hand: Hand, tricks: List<Trick>, seats: List<Seat>): Position? =
    winningPositionOfTrick(hand, tricks, seats)

fun winningPositionOfLastTrick(hand: Hand, tricks: List<Trick>, seats: List<Seat>): Position? {
    if (tricks.count() < 2) {
        return null
    }

    // Index 0 is the current trick, so we remove it to get data up to the last one
    return winningPositionOfTrick(hand, tricks.drop(1), seats)
}

/**
 * To figure this out we need to recursively loop all played tricks in the given hand. Figures out
 * the winner of the last trick in the tricks list.
 */
fun winningPositionOfTrick(hand: Hand, tricks: List<Trick>, seats: List<Seat>): Position? {
    if (hand.trump == null) {
        return null
    }

    var startPosition = seats.first { it.playerId == hand.startingPlayerId }.position

    // Tricks are order where index 0 is the newest trick, we need to start at the oldest so we reverse the list
    for (trick in tricks.reversed()) {
        val suitLed = trick.cardOf(startPosition)!!.suit
        val winningCard = trick.cards()
            .filter { it.suit == suitLed || it.suit == hand.trumpSuit() }
            .maxBy { cardValue(it, hand.trump) }

        startPosition = when (winningCard) {
            trick.north -> Position.NORTH
            trick.east -> Position.EAST
            trick.south -> Position.SOUTH
            trick.west -> Position.WEST
            else -> Position.NORTH  // since trick.cards are just cards from these positions we don't care
        }
    }

    return startPosition
}

/**
 * Get card value to compare which card is better - this has nothing to do with the
 * actual point values the players get.
 *
 * TODO: Uneufe/Obenabe
 */
fun cardValue(card: Card, trump: Trump): Int = when {
    trump.equalsSuit(card.suit) && card.rank == Rank.JACK -> 200 // Trump Buur
    trump.equalsSuit(card.suit) && card.rank == Rank.NINE -> 150 // Trump Nell
    trump.equalsSuit(card.suit) -> 100 + rankValue(card.rank, trump)
    else -> rankValue(card.rank, trump)
}

/**
 * TODO: Uneufe/Obenabe
 */
fun rankValue(rank: Rank, trump: Trump): Int = when (rank) {
    Rank.SIX -> 1
    Rank.SEVEN -> 2
    Rank.EIGHT -> 3
    Rank.NINE -> 4
    Rank.TEN -> 5
    Rank.JACK -> 6
    Rank.QUEEN -> 7
    Rank.KING -> 8
    Rank.ACE -> 9
    else -> 0
}

/**
 * See activePosition, this is just a helper to map a player to a position.
 */
fun activePlayer(hands: List<Hand>, players: List<Player>, seats: List<Seat>, tricks: List<Trick>): Player? =
    activePosition(hands, players, seats, tricks)?.let { playerAtPosition(it, seats, players) }

context(Raise<GameAlreadyFull>)
fun randomFreePosition(occupiedSeats: List<Seat>): Position {
    val occupied = occupiedSeats.map { it.position }.toSet()
    val all = Position.entries.toSet()

    return all.minus(occupied).randomOrNull() ?: raise(GameAlreadyFull)
}

fun startingPlayersSeatOfCurrentHand(hands: List<Hand>, players: List<Player>, seats: List<Seat>): Seat {
    val hand = currentHand(hands)!!
    val startingPlayer = players.firstOrNull { it.id == hand.startingPlayerId }!!

    return seats.first { it.playerId == startingPlayer.id }
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