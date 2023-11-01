package ch.yass

import ch.yass.core.CentrifugoClient
import ch.yass.core.pubsub.Channel
import ch.yass.core.pubsub.Message
import ch.yass.core.pubsub.publish
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.*

class PubSubTest : BaseTest() {

    @Test
    fun testPublishToChannel() {
        val mock = mockk<CentrifugoClient>()
        Yass.containerOverride(mock)

        val actions = listOf(Message("foo"))
        publish(actions, Channel("seat", UUID.randomUUID()))
        verify { mock.server.post(any<String>()) }
    }
}