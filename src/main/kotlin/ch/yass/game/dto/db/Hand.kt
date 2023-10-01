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
    val startingPlayerId: Int,
    val gameId: Int,
    val trump: Trump?,
    val gschobe: Boolean,
    val points: Int,
    val north: List<Card>,
    val east: List<Card>,
    val south: List<Card>,
    val west: List<Card>,
) {
    companion object {
        fun fromRecord(hand: HandRecord): Hand {
            return Hand(
                hand.id!!,
                UUID.fromString(hand.uuid)!!,
                hand.createdAt!!,
                hand.updatedAt!!,
                hand.startingPlayerId!!,
                hand.gameId!!,
                Trump.entries.firstOrNull { it.name == hand.trump },
                hand.gschobe!!,
                hand.points!!,
                listFromDbJson<Card>(hand.north),
                listFromDbJson<Card>(hand.east),
                listFromDbJson<Card>(hand.south),
                listFromDbJson<Card>(hand.west)
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

    /**
     * Helper so we can easily compare trumps with suits. Obviously
     * doesn't work for uneufe/obeabe.
     */
    fun trumpSuit(): Suit? {
        if (this.trump == null) {
            return null
        }

        return Suit.entries.firstOrNull { it.name == this.trump.name }
    }
}
