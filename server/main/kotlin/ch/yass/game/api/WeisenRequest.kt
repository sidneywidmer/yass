package ch.yass.game.api

import ch.yass.game.dto.Weis
import org.valiktor.functions.isNotNull
import org.valiktor.functions.matches
import org.valiktor.validate

data class WeisenRequest(
    val game: String,
    val weis: Weis
) {
    init {
        validate(this) {
            validate(WeisenRequest::game)
                .isNotNull()
                .matches(Regex("([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})"))
        }
    }
}