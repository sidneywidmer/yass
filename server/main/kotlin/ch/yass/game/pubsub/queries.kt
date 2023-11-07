package ch.yass.game.pubsub

import ch.yass.core.pubsub.Action
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.db.Seat

fun cardPlayedActions(state: GameState, card: Card, seat: Seat): List<Action> {
    return listOf(CardPlayed(card, seat.position))
}