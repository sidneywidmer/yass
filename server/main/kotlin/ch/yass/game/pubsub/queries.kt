package ch.yass.game.pubsub

import arrow.core.raise.fold
import ch.yass.core.error.GameError
import ch.yass.core.error.PlayerDoesNotOwnCard
import ch.yass.core.pubsub.Action
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.*
import ch.yass.game.dto.CardInHandState.*

fun newHandActions(state: GameState, seat: Seat): List<Action> {
    val points = pointsByPositionTotal(completedHands(state.hands, state.tricks), state.tricks, state.seats)
    val cards = cardsInHand(state, seat)
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)

    return listOf(
        UpdateState(nextState),
        UpdateActive(activePosition),
        UpdateHand(cards),
        UpdatePoints(points),
        UpdatePlayedCards(emptyList()),
    )
}

fun newTrickActions(state: GameState, seat: Seat): List<Action> {
    val cards = cardsInHand(state, seat)
    val hand = currentHand(state.hands)!!
    val tricks = tricksOfHand(state.tricks, hand)
    val winningPos = winningPositionOfLastTrick(hand, tricks, state.seats)
    val nextState = nextState(state)
    val activePosition = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)

    return listOf(
        UpdateState(nextState),
        UpdateActive(activePosition),
        UpdateHand(cards),
        UpdatePlayedCards(emptyList()),
        Message("Winning Position of last trick $winningPos"),
    )
}

fun cardPlayedActions(state: GameState, card: Card, playedBy: Seat, seat: Seat): List<Action> {
    val cards = cardsInHand(state, seat)
    val activePosition = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)
    val nextState = nextState(state)

    return listOf(
        UpdateState(nextState),
        UpdateActive(activePosition),
        CardPlayed(CardOnTable(card.suit, card.rank, card.skin, playedBy.position)),
        UpdateHand(cards),
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
        UpdateState(nextState),
        Message("Gschobe!")
    )
}

/**
 * Get all cards the given seat has currently in hand and are playable
 */
fun cardsInHand(state: GameState, seat: Seat): List<CardInHand> {
    val hand = currentHand(state.hands)!!
    val player = playerAtPosition(seat.position, state.seats, state.allPlayers)!!
    return hand.cardsOf(seat.position).map {
        val cardState = fold(
            { cardIsPlayable(it, player, state) },
            { error: GameError -> if (error is PlayerDoesNotOwnCard) ALREADY_PLAYED else UNPLAYABLE },
            { PLAYABLE }
        )
        CardInHand(it.suit, it.rank, it.skin, cardState)
    }
}

