package ch.yass.game.api

import org.valiktor.functions.*
import org.valiktor.validate

data class JoinGameRequest(
    val code: String
) {
    init {
        validate(this) {
            validate(JoinGameRequest::code).isNotEmpty().hasSize(5, 5).matches(Regex("([A-Z\\d]+)"))
        }
    }
}