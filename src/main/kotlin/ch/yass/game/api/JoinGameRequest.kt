package ch.yass.game.api

import org.valiktor.functions.contains
import org.valiktor.functions.hasSize
import org.valiktor.functions.isNotEmpty
import org.valiktor.validate

data class JoinGameRequest(
    val code: String
) {
    init {
        validate(this) {
            validate(JoinGameRequest::code).isNotEmpty().hasSize(5, 5).contains(Regex("([A-Z\\d]+)"))
        }
    }
}