package ch.yass.admin.api

import org.valiktor.functions.hasSize
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.matches
import org.valiktor.validate

data class MessageRequest(
    val message: String,
    val path: String,
    val gameUuid: String?, // message can be sent outside game
) {
    init {
        validate(this) {
            validate(MessageRequest::message).isNotEmpty().hasSize(1, 1024)
            validate(MessageRequest::path).isNotEmpty().hasSize(1, 255)
            validate(MessageRequest::gameUuid)
                .matches(Regex("([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})"))
        }
    }
}