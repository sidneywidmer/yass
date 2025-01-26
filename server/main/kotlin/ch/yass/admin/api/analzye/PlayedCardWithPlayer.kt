package ch.yass.admin.api.analzye

import ch.yass.game.dto.Card
import ch.yass.game.dto.Player

data class PlayedCardWithPlayer(
    val player: Player,
    val card: Card?
)
