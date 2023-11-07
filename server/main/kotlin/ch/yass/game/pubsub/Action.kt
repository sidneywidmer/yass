package ch.yass.game.pubsub

import ch.yass.core.pubsub.Action
import ch.yass.game.dto.Card
import ch.yass.game.dto.CardInHand
import ch.yass.game.dto.Points
import ch.yass.game.dto.Position

data class CardPlayed(val card: Card, val position: Position) : Action
data class UpdatePlayedCards(val cards: List<Card>) : Action
data class UpdateHand(val cards: List<CardInHand>) : Action
data class UpdateScore(val points: Points) : Action
data class UnlockPlayer(val unlocked: Boolean) : Action
data class Message(val message: String) : Action

