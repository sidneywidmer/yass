package ch.yass.game.dto.db

import ch.yass.core.helper.fromDbJson
import ch.yass.db.tables.records.GameRecord
import ch.yass.game.dto.GameSettings
import ch.yass.game.dto.GameStatus
import java.time.LocalDateTime
import java.util.*

data class Game(
    val id: Int,
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val code: String,
    val settings: GameSettings,
    val status: GameStatus
) {
    companion object {
        fun fromRecord(game: GameRecord): Game {
            return Game(
                game.id!!,
                UUID.fromString(game.uuid)!!,
                game.createdAt!!,
                game.updatedAt!!,
                game.code!!,
                fromDbJson<GameSettings>(game.settings),
                GameStatus.entries.first { it.name == game.status }
            )
        }
    }
}
