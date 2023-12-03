package ch.yass.game.api

import ch.yass.core.valiktor.isWinningConditionType
import org.valiktor.functions.isBetween
import org.valiktor.functions.isNotEmpty
import org.valiktor.validate

data class CreateCustomGameRequest(
    val botNorth: Boolean,
    val botEast: Boolean,
    val botSouth: Boolean,
    val botWest: Boolean,
    val winningConditionType: String,
    val winningConditionValue: Int
) {
    init {
        validate(this) {
            validate(CreateCustomGameRequest::winningConditionValue).isBetween(1, 10000) // Quick sanity check
            validate(CreateCustomGameRequest::winningConditionType).isNotEmpty().isWinningConditionType()
        }
    }
}