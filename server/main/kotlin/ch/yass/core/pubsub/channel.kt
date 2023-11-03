package ch.yass.core.pubsub

import ch.yass.core.helper.centrifugo
import ch.yass.core.helper.jackson
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import java.util.*

data class Channel(
    val namespace: String,
    val name: UUID
) {
    override fun toString(): String {
        return "${namespace}:#${name}"
    }
}

data class PublishRequest(
    val channel: String,
    val data: Any
)

fun publish(actions: List<Action>, channel: Channel) {
    val body = jackson().writeValueAsString(actions)
    val response = centrifugo().server.post("/api/publish")
        .body(body)
        .responseString()

    // TODO: Check response, maybe throw or log? How can we recover
}