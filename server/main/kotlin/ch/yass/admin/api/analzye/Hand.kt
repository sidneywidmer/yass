package ch.yass.admin.api.analzye

import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Points
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Player

data class Hand(
    val trump: Trump?,
    val gschobe: Gschobe,
    val startingPlayer: Player,
    val players: List<PlayerWithCards>,
    val tricks: List<TrickWithCards>,
    val points: Points
)
