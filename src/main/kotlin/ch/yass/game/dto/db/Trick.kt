package ch.yass.game.dto.db

import ch.yass.core.helper.fromDbJson
import ch.yass.db.tables.records.TrickRecord
import ch.yass.game.dto.Card
import ch.yass.game.dto.Position
import java.time.LocalDateTime
import java.util.*

data class Trick(
    val id: Int,
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val handId: Int,
    val winnerId: Int?,
    val points: Int,
    val north: Card?,
    val east: Card?,
    val south: Card?,
    val west: Card?,
) {
    companion object {
        fun fromRecord(trick: TrickRecord): Trick {
            return Trick(
                trick.id!!,
                UUID.fromString(trick.uuid)!!,
                trick.createdAt!!,
                trick.updatedAt!!,
                trick.handId!!,
                trick.winnerId,
                trick.points!!,
                fromDbJson(trick.north),
                fromDbJson(trick.east),
                fromDbJson(trick.south),
                fromDbJson(trick.west),
            )
        }
    }

    fun cardOf(position: Position): Card? {
        return when (position) {
            Position.NORTH -> this.north
            Position.EAST -> this.east
            Position.SOUTH -> this.south
            Position.WEST -> this.west
        }
    }

    fun cards(): List<Card> {
        return listOfNotNull(this.north, this.east, this.south, this.west)
    }
}