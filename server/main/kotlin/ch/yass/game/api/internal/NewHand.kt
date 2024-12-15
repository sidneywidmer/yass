package ch.yass.game.api.internal

import ch.yass.game.dto.Card
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Game
import java.util.EnumMap

data class NewHand(
    val game: Game,
    val startingPosition: Position,
    val positions: EnumMap<Position, List<Card>>,
    val trump: Trump? = null,
    val gschobe: Gschobe? = null
)