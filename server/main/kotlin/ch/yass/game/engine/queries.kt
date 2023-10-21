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
 * the case the starting player of the current hands card is the lead. We pass in the list of tricks so we can
 * also use this function the get the lead of an arbitrary trick within a hand and not just the last one.
 */
fun currentLeadPositionOfHand(hand: Hand, tricks: List<Trick>, seats: List<Seat>, allPlayers: List<Player>): Position =
    winningPositionOfLastTrick(hand, tricks, seats) ?: startingPlayersSeatOfHand(hand, allPlayers, seats).position

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
        !isAlreadyGschobe(hand) -> if (player.bot) State.SCHIEBE_BOT else State.SCHIEBE
        !isTrumpSet(hand) -> if (player.bot) State.TRUMP_BOT else State.TRUMP
        else -> if (player.bot) State.PLAY_CARD_BOT else State.PLAY_CARD
    }
}

fun nextHandStartingPosition(hands: List<Hand>, players: List<Player>, seats: List<Seat>): Position {
    val hand = currentHand(hands)!!
    val seat = startingPlayersSeatOfHand(hand, players, seats)
    return positionsOrderedWithStart(seat.position)[1]
}

fun nextHandStartingPlayer(hands: List<Hand>, players: List<Player>, seats: List<Seat>): Player {
    return playerAtPosition(nextHandStartingPosition(hands, players, seats), seats, players)!!
}

/**
 * Which player is expected to take the next action? If the trick is not complete yet, it's the person sitting
 * next to the person who has played a card last (starting with the lead player of the trick). If our trick
 * is complete already it's the starting player of the next hand.
 */
fun activePosition(
    hands: List<Hand>,
    players: List<Player>,
    seats: List<Seat>,
    tricks: List<Trick>
): Position {
    val hand = currentHand(hands)!!
    val tricksOfHand = tricksOfHand(tricks, hand)
    val trick = tricksOfHand(tricks, hand).first()

    val currentLead = currentLeadPositionOfHand(hand, tricksOfHand, seats, players)
    val positions = positionsOrderedWithStart(currentLead)

    // This is the first trick of the hand and the lead player has gschobe, it's their partners turn
    if (trick.cards().isEmpty() && tricksOfHand.count() == 1 && hand.gschobe == Gschobe.YES) {
        return positions[2]
    }

    if (trick.cards().count() < 4) {
        return positions.first { trick.cardOf(it) == null }
    }

    return nextHandStartingPosition(hands, players, seats)
}

fun winningPositionOfCurrentTrick(hand: Hand, tricks: List<Trick>, seats: List<Seat>): Position =
    winningPositionOfTricks(hand, tricks, seats)

fun winningPositionOfLastTrick(hand: Hand, tricks: List<Trick>, seats: List<Seat>): Position? {
    if (tricks.count() < 2) {
        return null
    }

    // Index 0 is the current trick, so we remove it to get data up to the last one
    return winningPositionOfTricks(hand, tricks.drop(1), seats)
}

fun winningPositionOfTrick(trick: Trick, lead: Position, trump: Trump): Position {
    val suitLed = trick.cardOf(lead)!!.suit
    val winningCard = trick.cards()
        .filter { it.suit == suitLed || it.suit == trump.toSuit() }
        .maxBy { cardValue(it, trump) }

    return when (winningCard) {
        trick.north -> Position.NORTH
        trick.east -> Position.EAST
        trick.south -> Position.SOUTH
        trick.west -> Position.WEST
        else -> Position.NORTH
    }
}

/**
 * To figure this out we need to recursively loop all played tricks in the given hand. Figures out
 * the winner of the last trick in the tricks list.
 */
fun winningPositionOfTricks(hand: Hand, tricks: List<Trick>, seats: List<Seat>): Position {
    val startPosition = seats.first { it.playerId == hand.startingPlayerId }.position

    return tricks.reversed().fold(startPosition) { lead, trick ->
        winningPositionOfTrick(trick, lead, hand.trump!!)
    }
}


/**
 * See activePosition, this is just a helper to map a player to a position.
 */
fun activePlayer(hands: List<Hand>, players: List<Player>, seats: List<Seat>, tricks: List<Trick>): Player? =
    playerAtPosition(activePosition(hands, players, seats, tricks), seats, players)

context(Raise<GameAlreadyFull>)
fun randomFreePosition(occupiedSeats: List<Seat>): Position {
    val occupied = occupiedSeats.map { it.position }.toSet()
    val all = Position.entries.toSet()

    return all.minus(occupied).randomOrNull() ?: raise(GameAlreadyFull)
}

fun startingPlayersSeatOfHand(hand: Hand, players: List<Player>, seats: List<Seat>): Seat {
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
        ) -> cards.filterNot { it.suit != hand.trump?.toSuit() && it.rank == Rank.JACK }

        else -> cards
    }

/**
 * Splits all tricks to a map with key POSITION and value a list of tricks that position won
 */
fun pointsByPosition(hand: Hand, tricks: List<Trick>, seats: List<Seat>): String {
    // loop all tricks, find out the winning position of the trick,
    // get card values by trump type (schwarz (clubs, spades) einfach, rot (heart, diamonds) doppelt)
    // uneufe obeabe (8=8, ace/6 = 11
    // Buur 20, Nell 9
    // Ass (11), König (4), Ober (3), Unter (2), Banner (10)
    // last trick +5
    val startPosition = seats.first { it.playerId == hand.startingPlayerId }.position
    val positionMap = Position.entries.associateWith { emptyList<Trick>() }
    val positionToTricksMap = tricks.reversed().fold(
        PositionToTrickAccumulator(positionMap, startPosition)
    ) { accumulator, trick ->
        val winner = winningPositionOfTrick(trick, accumulator.lead, hand.trump!!)
        val wonTricks = accumulator.positions[winner]!! + trick
        PositionToTrickAccumulator(accumulator.positions + (winner to wonTricks), winner)
    }

    val pointsMap: Map<Position, Int> = positionToTricksMap.positions.mapValues { (_, tricks) ->
        tricks.sumOf { foo(it) }
    }

    return "foo"
}

fun foo(trick: Trick): Int {
    return 5
}