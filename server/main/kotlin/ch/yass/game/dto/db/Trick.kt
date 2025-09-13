package ch.yass.game.dto.db

import ch.yass.core.helper.fromDbJson
import ch.yass.db.tables.records.TrickRecord
import ch.yass.game.dto.Card
import ch.yass.game.dto.CardOnTable
import ch.yass.game.dto.Position
import java.time.LocalDateTime
import java.util.*

data class Trick(
    val id: Int,
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val handId: Int,
    val north: Card?,
    val east: Card?,
    val south: Card?,
    val west: Card?,
) {
    companion object {
        fun fromRecord(trick: TrickRecord): Trick {
            return Trick(
                trick.id!!,
                UUID.fromString(trick.uuid),
                trick.createdAt,
                trick.updatedAt,
                trick.handId,
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

    fun cards(): List<Card> = listOfNotNull(this.north, this.east, this.south, this.west)

    fun cardsByPosition(): List<CardOnTable> {
        return Position.entries
            .mapNotNull { cardOf(it)?.let { card -> Pair(card, it) } }
            .map { p -> CardOnTable(p.first.suit, p.first.rank, p.first.skin, p.second) }
    }

    fun withCard(position: Position, card: Card): Trick {
        return when (position) {
            Position.NORTH -> this.copy(north = card)
            Position.EAST -> this.copy(east = card)
            Position.SOUTH -> this.copy(south = card)
            Position.WEST -> this.copy(west = card)
        }
    }
}