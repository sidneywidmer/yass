package ch.yass.identity.api

import ch.yass.game.dto.db.InternalPlayer
import java.util.*

data class WhoAmIResponse(
    val uuid: UUID,
    val name: String,
    val isAnon: Boolean
) {
    companion object {
        fun from(player: InternalPlayer): WhoAmIResponse = WhoAmIResponse(player.uuid, player.name, player.oryUuid == null)
    }
}

