package ch.yass.game.pubsub

import ch.yass.core.pubsub.Action
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.Player
import ch.yass.game.dto.db.InternalPlayer
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
    val lastHand = lastHand(state.hands) // in the game state the new hand is already created
    val player = playerAtPosition(seat.position, state.seats, state.allPlayers)
    val cards = cardsInHand(currentHand(state.hands), player, state)
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.seats, state.tricks)

    // For the welcome hand we don't need to look too hard, just move cards north it doesn't matter
    val winningPos = if (lastHand.trump == Trump.FREESTYLE) {
        Position.NORTH
    } else {
        winningPositionOfTricks(lastHand, tricksOfHand(state.tricks, lastHand))
    }

    return listOf(
        UpdateActive(activePosition),
        ClearPlayedCards(winningPos),
        UpdatePoints(points),
        UpdateState(nextState),
        UpdateHand(cards, true),
    )
}

fun gameFinishedActions(state: GameState): List<Action> {
    val points = pointsByPositionTotal(state.hands, state.tricks)
    val winners = getWinningTeam(points)
    val losers = getLosingTeam(points)

    return listOf(
        UpdatePoints(points),
        GameFinished(
            winners.team.positions.map { Player.from(playerAtPosition(it, state.seats, state.allPlayers)) },
            losers.team.positions.map { Player.from(playerAtPosition(it, state.seats, state.allPlayers)) },
            winners.points,
            losers.points
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
    val points = pointsByPositionTotal(state.hands, state.tricks)

    return listOf(
        UpdateActive(activePosition),
        ClearPlayedCards(winningPos!!),
        UpdatePoints(points),
        UpdateState(nextState),
        UpdateHand(cards, false),
    )
}

fun cardPlayedActions(state: GameState, card: Card, playedBy: Seat, seat: Seat): List<Action> {
    val player = playerAtPosition(seat.position, state.seats, state.allPlayers)
    val cards = cardsInHand(currentHand(state.hands), player, state)
    val activePosition = activePosition(state.hands, state.seats, state.tricks)
    val nextState = nextState(state)

    return listOf(
        UpdateActive(activePosition),
        UpdateState(nextState),
        CardPlayed(CardOnTable(card.suit, card.rank, card.skin, playedBy.position)),
        UpdateHand(cards, false),
    )
}

fun trumpChosenActions(state: GameState, trump: Trump, seat: Seat): List<Action> {
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.seats, state.tricks)
    val cards =
        currentHand(state.hands).cardsOf(seat.position) // We don't care about CardInHand since the state is wurst
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
    val points = pointsByPositionTotal(state.hands, state.tricks)

    val actions = listOf(
        UpdateActive(activePosition),
        UpdateState(nextState),
        UpdatePoints(points)
    ).toMutableList()

    // We don't need to show this weis to the player who just played it - they already know. Also, we don't need to
    // show other players that they would have possible weise but chose to skip
    if (seat != playedBy && weis.type != WeisType.SKIP) {
        actions.add(ShowWeis(playedBy.position, weis.toWeisWithPoints(currentHand(state.hands).trump)))
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
        ShowWeis(playedBy.position, weis.toWeisWithPoints(hand.trump)),
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

fun playerJoinedActions(state: GameState, newPlayer: InternalPlayer, joinedAtSeat: Seat): List<Action> {
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

fun playerDisconnectedActions(dcSeat: Seat, dcPlayer: InternalPlayer): List<Action> {
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