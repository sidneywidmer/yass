package ch.yass.identity.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.valiktor.functions.isNotNull
import org.valiktor.functions.matches
import org.valiktor.validate

@JsonIgnoreProperties(ignoreUnknown = true)
data class SubscribeRequest(
    val channel: String,
) {
    init {
        validate(this) {
            validate(SubscribeRequest::channel)
                .isNotNull()
                .matches(Regex("(seat:#[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})"))
        }
    }
}