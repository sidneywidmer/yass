package ch.yass.game.dto

import ch.yass.db.tables.references.PLAYER
import ch.yass.db.tables.references.SEAT
import org.jooq.Record
import java.util.*

data class PlayerAtTable(
    val uuid: UUID,
    val name: String,
    val bot: Boolean,
    val position: Position,
    val status: SeatStatus
) {
    companion object {
        fun fromRecord(record: Record): PlayerAtTable {
            return PlayerAtTable(
                UUID.fromString(record.get(PLAYER.UUID))!!,
                record.get(PLAYER.NAME)!!,
                record.get(PLAYER.BOT)!!,
                Position.entries.first { it.name == record.get(SEAT.POSITION) },
                SeatStatus.entries.first { it.name == record.get(SEAT.STATUS) },
            )
        }
    }
}
