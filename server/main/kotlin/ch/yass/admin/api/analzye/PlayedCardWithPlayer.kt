package ch.yass.admin.api.analzye

import ch.yass.game.dto.Card
import ch.yass.game.dto.db.Player

data class PlayedCardWithPlayer(
    val player: Player,
    val card: Card?
)
