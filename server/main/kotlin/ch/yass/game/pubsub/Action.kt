package ch.yass.game.pubsub

import ch.yass.core.pubsub.Action
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Seat

data class CardPlayed(val card: CardOnTable) : Action
data class ClearPlayedCards(val position: Position) : Action
data class UpdateState(val state: State) : Action
data class UpdateActive(val position: Position) : Action
data class UpdateHand(val cards: List<CardInHand>, val newCards: Boolean) : Action
data class UpdatePoints(val points: Points) : Action
data class UpdateTrump(val trump: Trump) : Action
data class PlayerJoined(val player: PlayerAtTable) : Action
data class Message(val message: String) : Action

