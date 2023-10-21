package ch.yass.admin.api.analzye

import ch.yass.game.dto.Suit
import ch.yass.game.dto.db.Player

data class TrickWithCards(
    val cards: List<PlayedCardWithPlayer>,
    val leadPlayer: Player?,
    val leadSuit: Suit?,
    val winnerPlayer: Player?
)
