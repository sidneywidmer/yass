package ch.yass.game.dto.db

import ch.yass.db.tables.records.TrickRecord
import ch.yass.db.tables.references.TRICK
import ch.yass.game.dto.Position
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
    val rejoinedAt: LocalDateTime?,
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
}
