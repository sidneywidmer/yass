package ch.yass.game.engine

import arrow.core.raise.Raise
import arrow.core.raise.fold
import ch.yass.core.error.GameAlreadyFull
import ch.yass.core.error.GameError
import ch.yass.core.error.PlayerDoesNotOwnCard
import ch.yass.core.helper.logger
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Seat
import ch.yass.game.dto.db.Trick
import kotlin.collections.contains

fun currentTrick(tricks: List<Trick>): Trick? = tricks.firstOrNull()

fun currentHand(hands: List<Hand>): Hand? = hands.firstOrNull()

fun lastHand(hands: List<Hand>): Hand? = hands.getOrNull(1)

/**
 * Hands that have 9 tricks with 4 cards each. Welcome hand is NOT counted since it only has 1 card and 1 trick.
 */
fun completedHands(hands: List<Hand>, tricks: List<Trick>): List<Hand> =
        hands.filter { hand -> tricksOfHand(tricks, hand).filter { trick -> trick.cards().size == 4 }.size == 9 }

fun playerSeat(player: Player, seats: List<Seat>): Seat =
        seats.first { it.playerId == player.id }

fun tricksOfHand(tricks: List<Trick>, hand: Hand): List<Trick> {
    return tricks.filter { it.handId == hand.id }
}

fun completeTricksOfHand(tricks: List<Trick>, hand: Hand): List<Trick> {
    return tricks.filter { it.handId == hand.id && it.cards().count() == 4 }
}

/**
 * Can return null because maybe the game is not full yet.
 */
fun playerAtPosition(position: Position, seats: List<Seat>, players: List<Player>): Player? {
    return seats.firstOrNull { it.position == position }
            .let { players.firstOrNull { player -> player.id == it?.playerId } }
}

fun cardsInHand(hand: Hand, player: Player, state: GameState): List<CardInHand> {
    val seat = playerSeat(player, state.seats)

    return hand.cardsOf(seat.position).map {
        val cardState = fold(
                { cardIsPlayable(it, player, state) },
                { error: GameError -> if (error is PlayerDoesNotOwnCard) CardInHandState.ALREADY_PLAYED else CardInHandState.UNPLAYABLE },
                { CardInHandState.PLAYABLE }
        )
        CardInHand(it.suit, it.rank, it.skin, cardState)
    }
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
    val position = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)
    val player = playerAtPosition(position, state.seats, state.allPlayers)!!
    val tricks = hand?.let { tricksOfHand(state.tricks, it) } ?: emptyList()

    return when {
        state.allPlayers.size < 4 -> State.WAITING_FOR_PLAYERS
        trick == null -> State.NEW_TRICK
        isGameFinished(state) -> State.FINISHED

        // Special case for "welcome" trick, only one card is played per player
        isWelcomeHandFinished(trick, state.hands) -> State.NEW_HAND

        isHandFinished(tricks) -> State.NEW_HAND
        isTrickFinished(trick) -> State.NEW_TRICK
        !isAlreadyGschobe(hand) -> if (player.bot) State.SCHIEBE_BOT else State.SCHIEBE
        !isTrumpSet(hand) -> if (player.bot) State.TRUMP_BOT else State.TRUMP
        !isAlreadyGewiesen(position, hand, tricks) -> State.WEISEN_FIRST
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

    // This is the first trick of the hand and the lead player has gschobe, it's their partners turn to choose
    // a trump. As soon as the trump is chosen it's the "normal" players turn again.
    if (trick.cards().isEmpty() && tricksOfHand.count() == 1 && hand.gschobe == Gschobe.YES && !isTrumpSet(hand)) {
        return positions[2]
    }

    if (trick.cards().count() < 4) {
        return positions.first { trick.cardOf(it) == null }
    }

    return nextHandStartingPosition(hands, players, seats)
}

fun winningPositionOfCurrentTrick(hand: Hand, tricks: List<Trick>, seats: List<Seat>): Position? {
    if (tricks.first().cards().size < 4) {
        return null
    }

    return winningPositionOfTricks(hand, tricks, seats)
}


fun winningPositionOfLastTrick(hand: Hand, tricks: List<Trick>, seats: List<Seat>): Position? {
    if (tricks.count() < 2) {
        return null
    }

    // Index 0 is the current trick, so we remove it to get data up to the last one
    val tricksWithoutNewest = tricks.drop(1)

    // It's possible that this function is called on an incomplete trick, e.g. for analyzing data. Because
    // of this we return early since winningPositionOfTricks only works with a complete trick.
    if (tricksWithoutNewest.first().cards().size < 4) {
        return null
    }

    return winningPositionOfTricks(hand, tricksWithoutNewest, seats)
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

fun pointsByPositionTotal(hands: List<Hand>, tricks: List<Trick>, seats: List<Seat>): Points {
    val weis = weisPointsByPositionTotal(hands)
    val card = cardPointsByPositionTotal(hands, tricks, seats)

    return Position.entries.associateWith { TotalPoints(card[it]!!, weis[it]!!) }
}

fun weisPointsByPositionTotal(hands: List<Hand>): SplitPoints {
    val initial = mapOf(
            Position.NORTH to 0,
            Position.EAST to 0,
            Position.SOUTH to 0,
            Position.WEST to 0,
    )

    return hands.fold(initial) { accumulator, hand ->
        val posToWeise = Position.entries.associateWith { pos -> hand.weiseOf(pos)!!.map { toWeisWithPoints(it, hand.trump!!) } }
        val posToPoints = posToWeise.mapValues { it.value.sumOf { weis -> weis.points } }.toMutableMap()
        val pointsNS = posToPoints[Position.NORTH]!! + posToPoints[Position.SOUTH]!!
        val pointsEW = posToPoints[Position.EAST]!! + posToPoints[Position.WEST]!!

        // TODO: Consider this!
        if (pointsNS == pointsEW) {
            // See: https://www.jasswelt.ch/blogs/neuigkeiten/weisregeln
            logger().error("Found identical Weis points for hands $hands - needs to be fixed depending on order played")
        }

        // Team NS has more weis-points, so the EW team makes 0 points
        if (pointsNS >= pointsEW) {
            posToPoints[Position.EAST] = 0
            posToPoints[Position.WEST] = 0
        }

        // Find the position having the STOECK Weis and if found, add the points to that position
        posToWeise.entries
                .flatMap { (pos, weise) -> weise.map { pos to it } }
                .firstOrNull { it.second.type == WeisType.STOECK }
                ?.let { posToPoints[it.first] = it.second.points }

        accumulator.mapValues { (position, points) -> points + posToPoints[position]!! }
    }
}

fun cardPointsByPositionTotal(hands: List<Hand>, tricks: List<Trick>, seats: List<Seat>): SplitPoints {
    val initial = mapOf(
            Position.NORTH to 0,
            Position.EAST to 0,
            Position.SOUTH to 0,
            Position.WEST to 0,
    )

    return hands.fold(initial) { accumulator, hand ->
        val tricksOfHand = completeTricksOfHand(tricks, hand)
        val pointsNew = cardPointsByPosition(hand, tricksOfHand, seats)
        accumulator.mapValues { (position, points) -> points + pointsNew[position]!! }
    }
}

/**
 * Splits all tricks to a map with key POSITION and value a list of tricks that position won. The given
 * tricks are ordered descending where index 0 is the newest trick, so we reverse the list.
 */
fun cardPointsByPosition(hand: Hand, tricks: List<Trick>, seats: List<Seat>): SplitPoints {
    val startPosition = seats.first { it.playerId == hand.startingPlayerId }.position
    val positionMap = Position.entries.associateWith { emptyList<Trick>() }
    val positionToTricksMap = tricks.reversed().fold(
            PositionToTrickAccumulator(positionMap, startPosition)
    ) { accumulator, trick ->
        val winner = winningPositionOfTrick(trick, accumulator.lead, hand.trump!!)
        val wonTricks = accumulator.positions[winner]!! + trick
        PositionToTrickAccumulator(accumulator.positions + (winner to wonTricks), winner)
    }

    return positionToTricksMap.positions.mapValues { (_, tricksOfPosition) ->
        tricksOfPosition.sumOf { trick ->
            // First means last played in this context, you get the bonus if the hand is complete
            val lastTrickBonus = if (trick.id == tricks.first().id && tricks.count() == 9) 5 else 0
            trick.cards().sumOf { card -> multiplyByTrump(cardPoints(card, hand.trump!!), hand.trump) } + lastTrickBonus
        }
    }
}

fun toWeisWithPoints(weis: Weis, trump: Trump): WeisWithPoints = WeisWithPoints(weis.type, weis.cards, multiplyByTrump(weisPoints(weis.type), trump))

fun possibleWeiseWithPoints(cards: List<Card>, trump: Trump): List<WeisWithPoints> =
        possibleWeise(cards, trump).map { toWeisWithPoints(it, trump) }

fun possibleWeise(cards: List<Card>, trump: Trump): List<Weis> =
        blattWeise(cards) + gleicheWeise(cards) + stoeckWeis(cards, trump)


