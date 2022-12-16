package ch.yass

import ch.yass.auth.dto.Player
import ch.yass.data.*
import ch.yass.db.tables.references.GAME
import ch.yass.db.tables.references.HAND
import ch.yass.db.tables.references.PLAYER
import ch.yass.game.dto.*
import com.fasterxml.jackson.databind.ObjectMapper
import junit.framework.TestCase.assertEquals
import org.jooq.DSLContext
import org.jooq.JSON
import org.jooq.impl.DSL.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import org.jooq.Records.mapping
import org.junit.Test
import org.koin.core.component.inject


class GreetingResourceTest : BaseTest() {

    private val dsl by inject<DSLContext>()
    private var mapper = ObjectMapper()

    @Test
    fun `insert into player column with timestmaps`() {
        val newPlayer = dsl.insertInto(
            PLAYER, PLAYER.UUID, PLAYER.NAME, PLAYER.CREATED_AT, PLAYER.UPDATED_AT
        ).values(
            UUID.randomUUID().toString(),
            "Player Name",
            LocalDateTime.now(ZoneOffset.UTC),
            LocalDateTime.now(ZoneOffset.UTC)
        ).returningResult(PLAYER.asterisk()).fetchOneInto(Player::class.java)

        assertEquals("Player Name", newPlayer!!.name)
    }

    @Test
    fun `work with json`() {
        val playerRecord =
            dsl.insertInto(PLAYER, PLAYER.UUID, PLAYER.NAME, PLAYER.CREATED_AT, PLAYER.UPDATED_AT).values(
                    UUID.randomUUID().toString(),
                    "Player Name",
                    LocalDateTime.now(ZoneOffset.UTC),
                    LocalDateTime.now(ZoneOffset.UTC)
                ).returningResult(PLAYER.ID).fetchOne()

        val gameRecord = dsl.insertInto(GAME, GAME.UUID, GAME.CREATED_AT, GAME.UPDATED_AT, GAME.CODE).values(
                UUID.randomUUID().toString(),
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
                "ABCD"
            ).returningResult(GAME.ID).fetchOne()

        val hand = setOf(
            Card(Suit.CLUBS, Rank.SIX),
            Card(Suit.CLUBS, Rank.SEVEN),
            Card(Suit.CLUBS, Rank.EIGHT),
            Card(Suit.CLUBS, Rank.NINE),
        )

        val handJson = mapper.writeValueAsString(hand)

        val newHandId = dsl.insertInto(
                HAND,
                HAND.UUID,
                HAND.CREATED_AT,
                HAND.UPDATED_AT,
                HAND.GAME_ID,
                HAND.STARTING_PLAYER_ID,
                HAND.TRUMP,
                HAND.GSCHOBE,
                HAND.POINTS,
                HAND.NORTH,
                HAND.EAST,
                HAND.SOUTH,
                HAND.WEST,
            ).values(
                UUID.randomUUID().toString(),
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
                gameRecord!!.getValue(GAME.ID),
                playerRecord!!.getValue(PLAYER.ID),
                Suit.CLUBS.ordinal,
                false,
                0,
                JSON.valueOf(handJson),
                JSON.valueOf(handJson),
                JSON.valueOf(handJson),
                JSON.valueOf(handJson),
            ).execute()
    }

    @Test
    fun `query related data and fetch into nested dataclass`() {
        val result = dsl.select(HAND.ID, HAND.UUID, row(
                HAND.player().ID,
                HAND.player().NAME,
                HAND.player().UUID,
                HAND.player().CREATED_AT,
                HAND.player().UPDATED_AT,
            ).mapping { id, name, uuid, d1, d2 ->
                Player(
                    id!!,
                    UUID.fromString(uuid),
                    name!!,
                    d1!!,
                    d2!!,
                )
            }).from(HAND).where(HAND.ID.eq(6)).fetchOne(mapping { i, s, p ->
                Hand(
                    i!!,
                    UUID.fromString(s),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    p!!,
                    Game(1, UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now(), ""),
                    "",
                    false,
                    0
                )
            })

        val foo = "ba"
    }

    @Test
    fun `insert many to many with data in through table`() {

    }

    @Test
    fun `log something`() {

    }

    @Test
    fun `checkout arrow and error handling`() {

    }

}
