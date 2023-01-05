package ch.yass.game.api

import ch.yass.game.dto.Card

data class GameStatePlayer(
    val name: String,
    val playedCard: Card? = null,
)