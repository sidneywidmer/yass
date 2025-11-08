package ch.yass.game.api

import ch.yass.core.valiktor.isRank
import ch.yass.core.valiktor.isSkin
import ch.yass.core.valiktor.isSuit
import org.valiktor.functions.isNotNull
import org.valiktor.functions.matches
import org.valiktor.functions.validate
import org.valiktor.validate

data class PlayCardRequest(
    val game: String,
    val card: PlayedCard
) {
    init {
        validate(this) {
            validate(PlayCardRequest::game)
                .isNotNull()
                .matches(Regex("([0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})"))

            validate(PlayCardRequest::card).validate {
                validate(PlayedCard::suit).isNotNull().isSuit()
                validate(PlayedCard::rank).isNotNull().isRank()
                validate(PlayedCard::skin).isSkin()
            }
        }
    }
}