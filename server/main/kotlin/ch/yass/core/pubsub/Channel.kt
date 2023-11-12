package ch.yass.core.pubsub

import java.util.*

data class Channel(
    val namespace: String,
    val name: UUID
) {
    override fun toString(): String {
        return "${namespace}:#${name}"
    }
}
