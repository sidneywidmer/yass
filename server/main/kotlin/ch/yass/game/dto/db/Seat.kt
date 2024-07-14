package ch.yass.game.dto.db

import ch.yass.db.tables.records.HandRecord
import ch.yass.db.tables.records.TrickRecord
import ch.yass.db.tables.references.HAND
import ch.yass.db.tables.references.TRICK
import ch.yass.game.dto.Position
import ch.yass.game.dto.SeatStatus
import org.jooq.JSON
import org.jooq.TableField
import java.time.LocalDateTime
import java.util.*

data class Seat(
    val id: Int,
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val playerId: Int,
    val gameId: Int,
    val position: Position,
    val status: SeatStatus,
    val rejoinedAt: LocalDateTime?,
    val playerPing: LocalDateTime,
) {
    /**
     * Helper to map this seats position to the correct db
     * column for the trick record.
     */
    fun trickColumn(): TableField<TrickRecord, JSON?> {
        return when (this.position) {
            Position.NORTH -> TRICK.NORTH
            Position.EAST -> TRICK.EAST
            Position.SOUTH -> TRICK.SOUTH
            Position.WEST -> TRICK.WEST
        }
    }

    fun weisColumn(): TableField<HandRecord, JSON?> {
        return when (this.position) {
            Position.NORTH -> HAND.NORTH_WEISE
            Position.EAST -> HAND.EAST_WEISE
            Position.SOUTH -> HAND.SOUTH_WEISE
            Position.WEST -> HAND.WEST_WEISE
        }
    }
}
