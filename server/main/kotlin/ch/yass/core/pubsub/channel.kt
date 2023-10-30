package ch.yass.core.pubsub

import ch.yass.core.helper.config
import ch.yass.core.helper.jackson
import ch.yass.core.helper.logger
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.github.kittinunf.fuel.httpPost
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
    val ptv = BasicPolymorphicTypeValidator.builder()
        .allowIfSubType("ch.yass.core.pubsub")
        .build()
    val mapper = jackson().copy().activateDefaultTypingAsProperty(
        ptv,
        ObjectMapper.DefaultTyping.NON_FINAL,
        "type"
    )
    val jsonBody = mapper.writeValueAsString(PublishRequest(channel.toString(), actions))
    val url = config().getString("centrifugo.basePath") + "/api/publish"

    logger().info("Publishing to centrifugo: ${jsonBody}, $url")

    val response = url.httpPost()
        .header("X-API-Key", config().getString("centrifugo.apiKey"))
        .body(jsonBody)
        .responseString()

    logger().info("Publishing response from centrifugo: ${response.second.statusCode}, ${response.third}")
}