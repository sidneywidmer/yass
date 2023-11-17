package ch.yass.game.pubsub

import ch.yass.core.pubsub.Action
import ch.yass.game.dto.*

data class CardPlayed(val card: CardOnTable) : Action
data class UpdatePlayedCards(val cards: List<CardOnTable>) : Action
data class UpdateState(val state: State) : Action
data class UpdateActive(val position: Position) : Action
data class UpdateHand(val cards: List<CardInHand>) : Action
data class UpdatePoints(val points: Points) : Action
data class UpdateTrump(val trump: Trump) : Action
data class Message(val message: String) : Action

