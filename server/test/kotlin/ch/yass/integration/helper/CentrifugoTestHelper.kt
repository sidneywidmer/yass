package ch.yass.integration.helper

import ch.yass.core.pubsub.Action
import ch.yass.game.pubsub.CardPlayed
import ch.yass.game.pubsub.ClearPlayedCards
import ch.yass.game.pubsub.GameFinished
import ch.yass.game.pubsub.PlayerDisconnected
import ch.yass.game.pubsub.PlayerJoined
import ch.yass.game.pubsub.ShowWeis
import ch.yass.game.pubsub.UpdateActive
import ch.yass.game.pubsub.UpdateHand
import ch.yass.game.pubsub.UpdatePoints
import ch.yass.game.pubsub.UpdatePossibleWeise
import ch.yass.game.pubsub.UpdateState
import ch.yass.game.pubsub.UpdateTrump
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import java.util.UUID

class CentrifugoTestHelper(val mapper: ObjectMapper) {

    fun parseActions(serveEvents: List<ServeEvent>): Map<String, List<Action>> {
        return serveEvents
            .filter { it.request.url.contains("/api/publish") }
            .mapNotNull { extractWebsocketActions(it) }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .mapValues { (_, lists) ->
                lists.flatten()
            }
    }

    private fun parseAction(node: JsonNode): Action {
        return when (node["type"].asText()) {
            "CardPlayed" -> mapper.treeToValue(node, CardPlayed::class.java)
            "ClearPlayedCards" -> mapper.treeToValue(node, ClearPlayedCards::class.java)
            "UpdateState" -> mapper.treeToValue(node, UpdateState::class.java)
            "UpdateActive" -> mapper.treeToValue(node, UpdateActive::class.java)
            "UpdateHand" -> mapper.treeToValue(node, UpdateHand::class.java)
            "UpdatePoints" -> mapper.treeToValue(node, UpdatePoints::class.java)
            "UpdateTrump" -> mapper.treeToValue(node, UpdateTrump::class.java)
            "UpdatePossibleWeise" -> mapper.treeToValue(node, UpdatePossibleWeise::class.java)
            "ShowWeis" -> mapper.treeToValue(node, ShowWeis::class.java)
            "PlayerJoined" -> mapper.treeToValue(node, PlayerJoined::class.java)
            "PlayerDisconnected" -> mapper.treeToValue(node, PlayerDisconnected::class.java)
            "GameFinished" -> mapper.treeToValue(node, GameFinished::class.java)
            else -> throw IllegalArgumentException("Unknown action type: ${node["type"].asText()}")
        }
    }

    private fun extractWebsocketActions(serveEvent: ServeEvent): Pair<String, List<Action>>? {
        val body = serveEvent.request.bodyAsString
        val rootNode = mapper.readTree(body)
        val channel = rootNode["channel"].asText()
        val actions = rootNode["data"].map { parseAction(it) }

        return channel to actions
    }

    fun assertActions(channelUuid: UUID, actions: Map<String, List<Action>>): ActionAssertions {
        val channelKey = "seat:#$channelUuid"
        return ActionAssertions(actions[channelKey] ?: emptyList())
    }
}