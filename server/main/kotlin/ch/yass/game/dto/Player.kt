package ch.yass.game.dto

import java.util.*
import ch.yass.game.dto.db.InternalPlayer

data class Player(
    val uuid: UUID,
    val name: String,
    val bot: Boolean
) {
    companion object {
        fun from(player: InternalPlayer): Player {
            return Player(player.uuid, player.name, player.bot)
        }
    }
}
