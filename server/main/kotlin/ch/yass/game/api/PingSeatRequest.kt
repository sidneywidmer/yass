package ch.yass.game.api

import org.valiktor.functions.isNotNull
import org.valiktor.functions.matches
import org.valiktor.validate

data class PingSeatRequest(
    val seat: String,
) {
    init {
        validate(this) {
            validate(PingSeatRequest::seat)
                .isNotNull()
                .matches(Regex("([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})"))
        }
    }
}