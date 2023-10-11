package ch.yass.game.api

import ch.yass.game.dto.Gschobe
import org.valiktor.functions.isIn
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.functions.matches
import org.valiktor.validate

data class SchiebeRequest(
    val game: String,
    val gschobe: String,
) {
    init {
        validate(this) {
            validate(SchiebeRequest::game)
                .isNotNull()
                .matches(Regex("([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})"))

            validate(SchiebeRequest::gschobe).isNotEmpty().isIn(Gschobe.YES.name, Gschobe.NO.name)
        }
    }
}