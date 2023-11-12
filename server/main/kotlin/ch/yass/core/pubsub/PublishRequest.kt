package ch.yass.core.pubsub

data class PublishRequest(
    val channel: String,
    val data: Any
)