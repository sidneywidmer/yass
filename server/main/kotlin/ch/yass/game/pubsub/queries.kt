package ch.yass.game.pubsub

import ch.yass.core.pubsub.Action
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.*

/**
 * Important: The list returned by each of these queries is not just by accident like it is. It's for example
 * crucial that the active player state is changed BEFORE the clients get notified about the actual
 * new state of the game. Since the order of a json array is preserved we don't have any problems,
 * but it has been a source of some hard to debug problems in the past.
 *
 * https://stackoverflow.com/questions/7214293/is-the-order-of-elements-in-a-json-list-preserved
 */

fun newHandActions(state: GameState, seat: Seat): List<Action> {
    val points = pointsByPositionTotal(completedHands(state.hands, state.tricks), state.tricks)
    val lastHand = lastHand(state.hands)
    val player = playerAtPosition(seat.position, state.seats, state.allPlayers)
    val cards = cardsInHand(currentHand(state.hands), player, state)
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.seats, state.tricks)

    // If no winner found it's probably the welcome hand, and we just move the cards to SOUTH as default
    val winningPos = winningPositionOfLastTrick(lastHand, tricksOfHand(state.tricks, lastHand)) ?: Position.SOUTH

    return listOf(
        UpdateActive(activePosition),
        UpdateState(nextState),
        UpdateHand(cards, true),
        UpdatePoints(points),
        ClearPlayedCards(winningPos),
    )
}

fun gameFinishedActions(state: GameState): List<Action> {
    val points = pointsByPositionTotal(completedHands(state.hands, state.tricks), state.tricks)
    val pairs = listOf(Pair(Position.NORTH, Position.SOUTH), Pair(Position.EAST, Position.WEST))

    // Calculate the sum of points for each pair
    val pointsSum = pairs.map { pair ->
        val sum = points[pair.first]!!.total() + points[pair.second]!!.total()
        Pair(pair, sum)
    }

    // Find the winners and losers
    val (maxPair, minPair) = pointsSum.maxBy { it.second } to pointsSum.minBy { it.second }
    val (winnerPositions, loserPositions) = maxPair.first.toList() to minPair.first.toList()
    val (maxPoints, minPoints) = maxPair.second to minPair.second

    return listOf(
        UpdatePoints(points),
        GameFinished(
            winnerPositions.map { position -> playerAtPosition(position, state.seats, state.allPlayers) },
            loserPositions.map { position -> playerAtPosition(position, state.seats, state.allPlayers) },
            maxPoints,
            minPoints
        )
    )
}

fun newTrickActions(state: GameState, seat: Seat): List<Action> {
    val hand = currentHand(state.hands)
    val player = playerAtPosition(seat.position, state.seats, state.allPlayers)
    val cards = cardsInHand(hand, player, state)
    val tricks = tricksOfHand(state.tricks, hand)
    val winningPos = winningPositionOfLastTrick(hand, tricks)
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.seats, state.tricks)

    return listOf(
        UpdateActive(activePosition),
        UpdateState(nextState),
        UpdateHand(cards, false),
        ClearPlayedCards(winningPos!!)
    )
}

fun cardPlayedActions(state: GameState, card: Card, playedBy: Seat, seat: Seat): List<Action> {
    val player = playerAtPosition(seat.position, state.seats, state.allPlayers)
    val cards = cardsInHand(currentHand(state.hands), player, state)
    val activePosition = activePosition(state.hands, state.seats, state.tricks)
    val nextState = nextState(state)
    val points = pointsByPositionTotal(state.hands, state.tricks)

    return listOf(
        UpdateActive(activePosition),
        UpdateState(nextState),
        CardPlayed(CardOnTable(card.suit, card.rank, card.skin, playedBy.position)),
        UpdateHand(cards, false),
        UpdatePoints(points)
    )
}

fun trumpChosenActions(state: GameState, trump: Trump, seat: Seat): List<Action> {
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.seats, state.tricks)
    val cards = currentHand(state.hands).cardsOf(seat.position) // We don't care about CardInHand since the state is wurst
    val weise = withoutStoeckPoints(possibleWeiseWithPoints(cards, trump))

    return listOf(
        UpdatePossibleWeise(weise),
        UpdateActive(activePosition),
        UpdateState(nextState),
        UpdateTrump(trump),
    )
}

fun gewiesenActions(state: GameState, weis: Weis, playedBy: Seat, seat: Seat): List<Action> {
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.seats, state.tricks)

    val actions = listOf(UpdateActive(activePosition), UpdateState(nextState)).toMutableList()

    // We don't need to show this weis to the player who just played it - they already know
    if (seat != playedBy) {
        actions.add(ShowWeis(playedBy.position, weis.toWeisWithPoints(currentHand(state.hands).trump!!)))
    }

    return actions
}

/**
 * Lightweight version of gewiesenActions, but we don't need to publish state update yet since this
 * is happening in the context of playing a card.
 */
fun stoeckGewiesenActions(hand: Hand, weis: Weis, playedBy: Seat, state: GameState): List<Action> {
    val points = pointsByPositionTotal(state.hands, state.tricks)
    return listOf(
        ShowWeis(playedBy.position, weis.toWeisWithPoints(hand.trump!!)),
        UpdatePoints(points)
    )
}

fun geschobenActions(state: GameState): List<Action> {
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.seats, state.tricks)

    return listOf(
        UpdateActive(activePosition),
        UpdateState(nextState)
    )
}

fun playerJoinedActions(state: GameState, newPlayer: Player, joinedAtSeat: Seat): List<Action> {
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.seats, state.tricks)

    return listOf(
        PlayerJoined(
            PlayerAtTable(
                newPlayer.uuid,
                newPlayer.name,
                newPlayer.bot,
                joinedAtSeat.position,
                joinedAtSeat.status
            )
        ),
        UpdateActive(activePosition),
        UpdateState(nextState)
    )
}

fun playerDisconnectedActions(dcSeat: Seat, dcPlayer: Player): List<Action> {
    return listOf(
        PlayerDisconnected(
            PlayerAtTable(
                dcPlayer.uuid,
                dcPlayer.name,
                dcPlayer.bot,
                dcSeat.position,
                dcSeat.status
            )
        )
    )
}