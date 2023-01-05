package ch.yass.game.api

import ch.yass.game.dto.Card
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import java.time.LocalDateTime
import java.util.UUID

data class GameState(
    val gameUUID: UUID,
    val players: List<GameStatePlayer>, // isCurrentTurn yes/no, position, cardCount, all except you
    val trump: Trump?,
    val hand: List<Card>, // Locked, unlocked
    val position: Position,
    val rejoinedAt: LocalDateTime?
)