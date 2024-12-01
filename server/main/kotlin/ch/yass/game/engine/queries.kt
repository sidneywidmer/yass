package ch.yass.game.engine

import arrow.core.raise.Raise
import arrow.core.raise.fold
import ch.yass.core.error.GameAlreadyFull
import ch.yass.core.error.GameError
import ch.yass.core.error.PlayerDoesNotOwnCard
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

fun positionSeat(position: Position, seats: List<Seat>): Seat =
    seats.first { it.position == position }

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
fun currentLeadPositionOfHand(hand: Hand, tricks: List<Trick>, seats: List<Seat>): Position =
    winningPositionOfLastTrick(hand, tricks) ?: startingPlayersSeatOfHand(hand, seats).position

fun nextState(state: GameState): State {
    val trick = currentTrick(state.tricks)
    val hand = currentHand(state.hands)
    val position = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)
    val player = playerAtPosition(position, state.seats, state.allPlayers)!!
    val tricks = hand?.let { tricksOfHand(state.tricks, it) } ?: emptyList()
    val weise = hand?.trump?.let { possibleWeise(hand.cardsOf(position), it) }.orEmpty()

    // The order of these checks is VERY relevant
    return when {
        state.allPlayers.size < 4 -> State.WAITING_FOR_PLAYERS
        trick == null -> State.NEW_TRICK
        hand == null -> State.NEW_HAND
        isGameFinished(state) -> State.FINISHED

        // Special case for "welcome" trick, only one card is played per player
        isWelcomeHandFinished(trick, state.hands) -> State.NEW_HAND

        !isAlreadyGewiesenSecond(
            tricks,
            hand,
            state.seats
        ) -> State.WEISEN_SECOND  // check for second weis before dealing new hand
        isHandFinished(tricks) -> State.NEW_HAND
        isTrickFinished(trick) -> State.NEW_TRICK
        !isAlreadyGschobe(hand) -> if (player.bot) State.SCHIEBE_BOT else State.SCHIEBE
        !isTrumpSet(hand) -> if (player.bot) State.TRUMP_BOT else State.TRUMP
        !isAlreadyGewiesen(
            position,
            hand,
            tricks,
            withoutStoeck(weise)
        ) -> if (player.bot) State.WEISEN_FIRST_BOT else State.WEISEN_FIRST

        else -> if (player.bot) State.PLAY_CARD_BOT else State.PLAY_CARD
    }
}

fun nextHandStartingPosition(hands: List<Hand>, players: List<Player>, seats: List<Seat>): Position {
    val hand = currentHand(hands)!!
    val seat = startingPlayersSeatOfHand(hand, seats)
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

    val currentLead = currentLeadPositionOfHand(hand, tricksOfHand, seats)
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

    return winningPositionOfTricks(hand, tricks)
}


fun winningPositionOfLastTrick(hand: Hand, tricks: List<Trick>): Position? {
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

    return winningPositionOfTricks(hand, tricksWithoutNewest)
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
fun winningPositionOfTricks(hand: Hand, tricks: List<Trick>): Position =
    tricks.reversed().fold(hand.startingPosition) { lead, trick -> winningPositionOfTrick(trick, lead, hand.trump!!) }

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

fun startingPlayersSeatOfHand(hand: Hand, seats: List<Seat>): Seat =
    seats.first { it.position == hand.startingPosition }

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
    val weis = weisPointsByPositionTotal(hands, tricks, seats)
    val card = cardPointsByPositionTotal(hands, tricks, seats)

    return Position.entries.associateWith { TotalPoints(card[it]!!, weis[it]!!) }
}

fun weisPointsByPositionTotal(hands: List<Hand>, tricks: List<Trick>, seats: List<Seat>): SplitPoints {
    val initial = mapOf(
        Position.NORTH to 0,
        Position.EAST to 0,
        Position.SOUTH to 0,
        Position.WEST to 0,
    )

    return hands.fold(initial) { accumulator, hand ->
        val validForWeis = tricksOfHand(tricks, hand).any { it.cards().size == 4 }
        // Don't count the weis points of this hand. This could be the case if not yet all players had a chance
        // to show their weis. After the first trick has 4 cards we know for a fact that everyone had their chance.
        if (!validForWeis) return@fold accumulator

        val startingSeat = startingPlayersSeatOfHand(hand, seats)
        val posToWeise =
            Position.entries.associateWith { pos -> hand.weiseOf(pos).map { it.toWeisWithPoints(hand.trump!!) } }
        val posToPoints = posToWeise.mapValues { it.value.sumOf { weis -> weis.points } }.toMutableMap()
        val teamToPoints = Team.entries.associateWith { it.positions.sumOf { pos -> posToPoints[pos]!! } }

        // Only the team with most weis points gets them. If they have the same amount,
        // the team with the starting player wins.
        if (teamToPoints[Team.NS]!! < teamToPoints[Team.EW]!!) {
            Team.NS.positions.map { posToPoints.put(it, 0) }
        } else if (teamToPoints[Team.NS]!! > teamToPoints[Team.EW]!!) {
            Team.EW.positions.map { posToPoints.put(it, 0) }
        } else {
            Team.entries.first { startingSeat.position !in it.positions }.positions.map { posToPoints.put(it, 0) }
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
        val pointsNew = cardPointsByPosition(hand, tricksOfHand)
        accumulator.mapValues { (position, points) -> points + pointsNew[position]!! }
    }
}

/**
 * Splits all tricks to a map with key POSITION and value a list of tricks that position won. The given
 * tricks are ordered descending where index 0 is the newest trick, so we reverse the list.
 */
fun cardPointsByPosition(hand: Hand, tricks: List<Trick>): SplitPoints {
    val positionMap = Position.entries.associateWith { emptyList<Trick>() }
    val positionToTricksMap = tricks.reversed().fold(
        PositionToTrickAccumulator(positionMap, hand.startingPosition)
    ) { accumulator, trick ->
        val winner = winningPositionOfTrick(trick, accumulator.lead, hand.trump!!)
        val wonTricks = accumulator.positions[winner]!! + trick
        PositionToTrickAccumulator(accumulator.positions + (winner to wonTricks), winner)
    }

    return positionToTricksMap.positions.mapValues { (_, tricksOfPosition) ->
        tricksOfPosition.sumOf { trick ->
            // First means last played in this context, you get the bonus if the hand is complete
            val bonus = multiplyByTrump(5, hand.trump!!)
            val lastTrickBonus = if (trick.id == tricks.first().id && tricks.count() == 9) bonus else 0
            trick.cards().sumOf { card -> multiplyByTrump(cardPoints(card, hand.trump), hand.trump) } + lastTrickBonus
        }
    }
}

fun Weis.toWeisWithPoints(trump: Trump): WeisWithPoints =
    WeisWithPoints(this.type, this.cards, multiplyByTrump(weisPoints(this.type), trump))

fun possibleWeiseWithPoints(cards: List<Card>, trump: Trump): List<WeisWithPoints> =
    possibleWeise(cards, trump).map { it.toWeisWithPoints(trump) }

fun possibleWeise(cards: List<Card>, trump: Trump): List<Weis> =
    blattWeise(cards) + gleicheWeise(cards) + stoeckWeis(cards, trump)

/**
 * Can't have the same name since generics get erased at runtime.
 */
fun withoutStoeckPoints(weise: List<WeisWithPoints>): List<WeisWithPoints> =
    weise.filter { w -> w.type != WeisType.STOECK }

fun withoutStoeck(weise: List<Weis>): List<Weis> = weise.filter { w -> w.type != WeisType.STOECK }

fun weisWinner(hand: Hand, tricks: List<Trick>, seats: List<Seat>): List<Position> {
    val points = weisPointsByPositionTotal(listOf(hand), tricks, seats)
    return Team.entries
        .associateWith { it.positions.sumOf { pos -> points[pos]!! } }
        .maxBy { it.value }
        .key.positions
}

fun remainingWeise(hand: Hand): Map<Position, List<Weis>> {
    val playedWeise = Position.entries.associateWith { withoutStoeck(hand.weiseOf(it)) }
    val possibleWeise = Position.entries.associateWith { withoutStoeck(possibleWeise(hand.cardsOf(it), hand.trump!!)) }

    return possibleWeise.mapValues { (position, weise) -> weise.filterNot { playedWeise[position]!!.contains(it) } }
}