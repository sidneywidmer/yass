package ch.yass.admin.dsl

import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump

data class GameStateDSL(val players: List<PlayerDSL>, val hands: List<HandDSL>)
data class PlayerDSL(val name: String, val bot: Boolean, val position: Position)
data class TrickDSL(val north: String?, val east: String?, val south: String?, val west: String?)
data class HandPositionDSL(val cards: String, val start: Boolean = false, val position: Position)
data class HandDSL(
    val trump: Trump?,
    val gschobe: Gschobe,
    val north: HandPositionDSL,
    val east: HandPositionDSL,
    val south: HandPositionDSL,
    val west: HandPositionDSL,
    val tricks: List<TrickDSL>
) {
    fun positions(): List<HandPositionDSL> {
        return listOfNotNull(north, east, south, west)
    }
}