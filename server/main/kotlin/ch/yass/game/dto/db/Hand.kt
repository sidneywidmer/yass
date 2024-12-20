package ch.yass.game.dto.db

import ch.yass.core.helper.listFromDbJson
import ch.yass.db.tables.records.HandRecord
import ch.yass.game.dto.*
import java.time.LocalDateTime
import java.util.*

data class Hand(
    val id: Int,
    val uuid: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val startingPosition: Position,
    val gameId: Int,
    val trump: Trump,
    val gschobe: Gschobe,
    val north: List<Card>,
    val east: List<Card>,
    val south: List<Card>,
    val west: List<Card>,
    val northWeise: List<Weis>,
    val eastWeise: List<Weis>,
    val southWeise: List<Weis>,
    val westWeise: List<Weis>,
) {
    companion object {
        fun fromRecord(hand: HandRecord): Hand {
            return Hand(
                hand.id!!,
                UUID.fromString(hand.uuid),
                hand.createdAt,
                hand.updatedAt,
                Position.entries.first { it.name == hand.startingPosition },
                hand.gameId,
                Trump.entries.first { it.name == hand.trump },
                Gschobe.entries.first { it.name == hand.gschobe },
                listFromDbJson<Card>(hand.north),
                listFromDbJson<Card>(hand.east),
                listFromDbJson<Card>(hand.south),
                listFromDbJson<Card>(hand.west),
                listFromDbJson<Weis>(hand.northWeise),
                listFromDbJson<Weis>(hand.eastWeise),
                listFromDbJson<Weis>(hand.southWeise),
                listFromDbJson<Weis>(hand.westWeise)
            )
        }
    }

    fun cardsOf(position: Position): List<Card> {
        return when (position) {
            Position.NORTH -> this.north
            Position.EAST -> this.east
            Position.SOUTH -> this.south
            Position.WEST -> this.west
        }
    }

    fun weiseOf(position: Position): List<Weis> {
        return when (position) {
            Position.NORTH -> this.northWeise
            Position.EAST -> this.eastWeise
            Position.SOUTH -> this.southWeise
            Position.WEST -> this.westWeise
        }
    }

}
