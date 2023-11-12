package ch.yass.game.pubsub

import arrow.core.raise.recover
import ch.yass.core.pubsub.Action
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.CardInHand
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.*

fun cardPlayedActions(state: GameState, card: Card, playedBy: Seat, seat: Seat): List<Action> {
    val hand = currentHand(state.hands)!!
    val player = playerAtPosition(seat.position, state.seats, state.allPlayers)!!
    val tricksOfHand = completeTricksOfHand(state.tricks, hand) // newest trick is index 0
    val points = pointsByPosition(hand, tricksOfHand, state.seats)
    val cards = hand.cardsOf(seat.position).map {
        val notLocked = recover({ cardIsPlayable(it, player, state) }, { false })
        CardInHand(it.suit, it.rank, it.skin, !notLocked)
    }

    return listOf(
        CardPlayed(card, playedBy.position),
        UpdateHand(cards),
        UpdateScore(points)
    )
}