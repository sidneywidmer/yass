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
)
