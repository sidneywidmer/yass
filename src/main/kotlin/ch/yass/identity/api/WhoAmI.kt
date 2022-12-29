package ch.yass.identity.api

import ch.yass.game.dto.Player
import java.util.*

data class WhoAmI(
    val uuid: UUID,
    val name: String,
) {
    companion object {
        fun from(player: Player): WhoAmI {
            return WhoAmI(player.uuid, player.name)
        }
    }
}

