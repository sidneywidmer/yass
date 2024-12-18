package ch.yass.identity.api

import ch.yass.game.dto.db.Player
import java.util.*

data class WhoAmIResponse(
    val uuid: UUID,
    val name: String,
) {
    companion object {
        fun from(player: Player): WhoAmIResponse = WhoAmIResponse(player.uuid, player.name)
    }
}

