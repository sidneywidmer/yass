package ch.yass.game.api

import ch.yass.core.valiktor.isTrump
import org.valiktor.functions.*
import org.valiktor.validate

data class ChooseTrumpRequest(
    val game: String,
    val trump: String
) {
    init {
        validate(this) {
            validate(ChooseTrumpRequest::game)
                .isNotNull()
                .matches(Regex("([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})"))

            validate(ChooseTrumpRequest::trump).isNotNull().isTrump()
        }
    }
}