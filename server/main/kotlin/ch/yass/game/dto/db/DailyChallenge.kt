package ch.yass.game.dto.db

import ch.yass.core.helper.fromDbJson
import ch.yass.db.tables.records.DailyChallengeRecord
import ch.yass.game.dto.Card
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class DailyChallenge(
    val id: Int,
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val day: LocalDate,
    val seed: Long,
    val forcedDecks: List<List<Card>>
) {
    companion object {
        fun fromRecord(challenge: DailyChallengeRecord): DailyChallenge {
            return DailyChallenge(
                challenge.id,
                UUID.fromString(challenge.uuid),
                challenge.createdAt,
                challenge.updatedAt,
                challenge.day,
                challenge.seed,
                // TODO: Arrays instead of lists, they keep their component type at runtime
                fromDbJson<Array<Array<Card>>>(challenge.forcedDecks).map { it.toList() }
            )
        }
    }
}
