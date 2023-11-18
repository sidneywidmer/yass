package ch.yass.core.pubsub

import ch.yass.core.CentrifugoClient
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody


class PubSub(private val jackson: ObjectMapper, private val centrifugo: CentrifugoClient) {
    fun publish(actions: List<Action>, channel: Channel) {
        val body = jackson.writeValueAsString(PublishRequest(channel.toString(), actions))

        val request = Request.Builder()
            .url("/api/publish")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        centrifugo.server.newCall(request).execute()
    }
}

