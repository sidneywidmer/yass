package ch.yass.game.pubsub

import ch.yass.core.pubsub.Action
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.CardOnTable
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.*

fun newHandActions(state: GameState, seat: Seat): List<Action> {
    val points = pointsByPositionTotal(completedHands(state.hands, state.tricks), state.tricks, state.seats)
    val hand = currentHand(state.hands)!!
    val lastHand = lastHand(state.hands)!!
    val player = playerAtPosition(seat.position, state.seats, state.allPlayers)!!
    val cards = cardsInHand(hand, player, state)
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)

    // If no winner found it's probably the welcome hand, and we just move the cords to SOUTH as default
    val winningPos =
        winningPositionOfLastTrick(lastHand, tricksOfHand(state.tricks, lastHand), state.seats) ?: Position.SOUTH

    return listOf(
        UpdateState(nextState),
        UpdateActive(activePosition),
        UpdateHand(cards, true),
        UpdatePoints(points),
        ClearPlayedCards(winningPos),
    )
}

fun newTrickActions(state: GameState, seat: Seat): List<Action> {
    val hand = currentHand(state.hands)!!
    val player = playerAtPosition(seat.position, state.seats, state.allPlayers)!!
    val cards = cardsInHand(hand, player, state)
    val tricks = tricksOfHand(state.tricks, hand)
    val winningPos = winningPositionOfLastTrick(hand, tricks, state.seats)
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)

    return listOf(
        UpdateState(nextState),
        UpdateActive(activePosition),
        UpdateHand(cards, false),
        ClearPlayedCards(winningPos!!)
    )
}

fun cardPlayedActions(state: GameState, card: Card, playedBy: Seat, seat: Seat): List<Action> {
    val hand = currentHand(state.hands)!!
    val player = playerAtPosition(seat.position, state.seats, state.allPlayers)!!
    val cards = cardsInHand(hand, player, state)
    val activePosition = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)
    val nextState = nextState(state)

    return listOf(
        UpdateState(nextState),
        UpdateActive(activePosition),
        CardPlayed(CardOnTable(card.suit, card.rank, card.skin, playedBy.position)),
        UpdateHand(cards, false),
    )
}

fun trumpChosenActions(state: GameState, trump: Trump, seat: Seat): List<Action> {
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)

    return listOf(
        UpdateActive(activePosition),
        UpdateState(nextState),
        UpdateTrump(trump),
    )
}

fun schiebeActions(state: GameState, seat: Seat): List<Action> {
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)

    return listOf(
        UpdateActive(activePosition),
        UpdateState(nextState)
    )
}

fun playerJoinedActions(state: GameState, newPlayer: Player, seat: Seat): List<Action> {
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)

    return listOf(
        UpdateActive(activePosition),
        UpdateState(nextState)
    )
}