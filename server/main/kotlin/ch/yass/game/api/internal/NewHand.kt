package ch.yass.game.api.internal

import ch.yass.game.dto.Card
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.Player

data class NewHand(
    val game: Game,
    val startingPlayer: Player,
    val positions: Map<Position, List<Card>>,
    val trump: Trump? = null,
    val gschobe: Gschobe? = null
)