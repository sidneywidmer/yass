package ch.yass.game.dto

import ch.yass.game.dto.db.Game
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

data class RunningGame(
    val uuid: UUID,
    val code: String,
    val kind: GameKind,
    val startedAt: Instant
) {
    companion object {
        fun from(game: Game): RunningGame =
            RunningGame(game.uuid, game.code, game.kind, game.createdAt.toInstant(ZoneOffset.UTC))
    }
}
