package ch.yass.core.pubsub

import ch.yass.core.CentrifugoClient
import com.fasterxml.jackson.databind.ObjectMapper


class PubSub(private val jackson: ObjectMapper, private val centrifugo: CentrifugoClient) {
    fun publish(actions: List<Action>, channel: Channel) {
        val body = jackson.writeValueAsString(PublishRequest(channel.toString(), actions))
        val response = centrifugo.server.post("/api/publish")
            .body(body)
            .responseString()

        // TODO: Check response, maybe throw or log? How can we recover
    }
}

