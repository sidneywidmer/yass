package ch.yass.game

import arrow.core.raise.Raise
import ch.yass.core.error.GameAlreadyFull
import ch.yass.core.error.GameNotFound
import ch.yass.core.error.GameWithCodeNotFound
import ch.yass.core.error.SeatNotFound
import ch.yass.core.helper.toDbJson
import ch.yass.db.tables.references.*
import ch.yass.game.api.internal.GameState
import ch.yass.game.api.internal.NewHand
import ch.yass.game.api.internal.NewSeat
import ch.yass.game.dto.*
import ch.yass.game.dto.db.*
import ch.yass.game.dto.db.Game
import ch.yass.game.engine.randomFreePosition
import org.jooq.DSLContext
import org.jooq.Records.mapping
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class GameRepository(private val db: DSLContext) {

    fun createGame(settings: GameSettings): Game {
        return db.insertInto(GAME, GAME.UUID, GAME.CODE, GAME.CREATED_AT, GAME.UPDATED_AT, GAME.SETTINGS, GAME.STATUS)
            .values(
                UUID.randomUUID().toString(),
                (1..5).map { ('A'..'Z').random() }.joinToString(""), // TODO: Handle collisions
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
                toDbJson(settings),
                GameStatus.RUNNING.name
            )
            .returningResult(GAME)
            .fetchOne(mapping(Game::fromRecord))!!
    }

    context(Raise<GameAlreadyFull>)
    fun takeASeat(game: Game, player: Player, position: Position? = null): Seat {
        val seats = getSeats(game)
        val maybeAlreadySeated = seats.firstOrNull { it.playerId == player.id }
        return maybeAlreadySeated?.let { rejoinSeat(it) }
            ?: run {
                val p = position ?: randomFreePosition(seats)
                createSeat(NewSeat(game, player, p))
            }
    }

    /**
     * Collect all the info we have about a game and pass it to the engine which is responsible
     * to interpret the given data.
     */
    fun getState(game: Game): GameState {
        val seats = getSeats(game)

        // Currently the first player to join a room is also starting the game, maybe randomize?
        // I also don't like that we pass the player just to create the first hand...
        val hands = getHands(game)
        val tricks = getTricks(hands.map { it.id })
        val players = getPlayers(seats)

        return GameState(game, players, seats, hands, tricks)
    }

    context(Raise<GameWithCodeNotFound>)
    fun getByCode(code: String): Game =
        db.selectFrom(GAME)
            .where(GAME.CODE.eq(code))
            .fetchOneInto(Game::class.java) ?: raise(GameWithCodeNotFound(code))

    context(Raise<GameNotFound>)
    fun getByUUID(uuid: String): Game {
        val game = db.selectFrom(GAME)
            .where(GAME.UUID.eq(uuid))
            .fetchOneInto(Game::class.java)

        return game ?: raise(GameNotFound(uuid))
    }

    context(Raise<SeatNotFound>)
    fun getBySeatUUID(uuid: String): Game {
        val game = db.select()
            .from(SEAT)
            .join(GAME).on(SEAT.GAME_ID.eq(GAME.ID))
            .where(SEAT.UUID.eq(uuid))
            .fetchOneInto(Game::class.java)

        return game ?: raise(SeatNotFound(uuid))
    }

    fun chooseTrump(trump: Trump, hand: Hand): Hand =
        db.update(HAND)
            .set(HAND.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(HAND.TRUMP, trump.name)
            .where(HAND.ID.eq(hand.id))
            .returningResult(HAND)
            .fetchOneInto(Hand::class.java)!!

    fun updateSeatStatus(seat: Seat, status: SeatStatus): Seat =
        db.update(SEAT)
            .set(SEAT.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(SEAT.STATUS, status.name)
            .where(SEAT.ID.eq(seat.id))
            .returningResult(SEAT)
            .fetchOneInto(Seat::class.java)!!

    fun schiebe(gschobe: Gschobe, hand: Hand): Hand =
        db.update(HAND)
            .set(HAND.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(HAND.GSCHOBE, gschobe.name)
            .where(HAND.ID.eq(hand.id))
            .returningResult(HAND)
            .fetchOneInto(Hand::class.java)!!

    fun playCard(card: Card, trick: Trick, seat: Seat): Trick =
        db.update(TRICK)
            .set(TRICK.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(seat.trickColumn(), toDbJson(card))
            .where(TRICK.ID.eq(trick.id))
            .returningResult(TRICK)
            .fetchOneInto(Trick::class.java)!!

    fun createTrick(hand: Hand): Trick =
        db.insertInto(TRICK, TRICK.UUID, TRICK.CREATED_AT, TRICK.UPDATED_AT, TRICK.HAND_ID)
            .values(
                UUID.randomUUID().toString(),
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
                hand.id
            )
            .returningResult(TRICK)
            .fetchOneInto(Trick::class.java)!!

    fun createHand(hand: NewHand): Hand =
        db.insertInto(
            HAND,
            HAND.UUID,
            HAND.CREATED_AT,
            HAND.UPDATED_AT,
            HAND.GAME_ID,
            HAND.STARTING_PLAYER_ID,
            HAND.TRUMP,
            HAND.GSCHOBE,
            HAND.NORTH,
            HAND.EAST,
            HAND.SOUTH,
            HAND.WEST
        )
            .values(
                UUID.randomUUID().toString(),
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
                hand.game.id,
                hand.startingPlayer.id,
                hand.trump?.name,
                hand.gschobe?.name ?: Gschobe.NOT_YET.name,
                toDbJson(hand.positions[Position.NORTH]),
                toDbJson(hand.positions[Position.EAST]),
                toDbJson(hand.positions[Position.SOUTH]),
                toDbJson(hand.positions[Position.WEST]),
            )
            .returningResult(HAND)
            .fetchOne(mapping(Hand::fromRecord))!!

    fun pingSeat(seat: Seat, ping: LocalDateTime): Seat {
        return db.update(SEAT)
            .set(SEAT.PLAYER_PING, ping)
            .where(SEAT.ID.eq(seat.id))
            .returningResult(SEAT)
            .fetchOneInto(Seat::class.java)!!
    }

    fun finishGame(game: Game): Game {
        return db.update(GAME)
            .set(GAME.STATUS, GameStatus.FINISHED.name)
            .where(GAME.ID.eq(game.id))
            .returningResult(GAME)
            .fetchOneInto(Game::class.java)!!
    }

    private fun createSeat(seat: NewSeat): Seat {
        return db
            .insertInto(
                SEAT,
                SEAT.UUID,
                SEAT.PLAYER_ID,
                SEAT.GAME_ID,
                SEAT.POSITION,
                SEAT.PLAYER_PING,
                SEAT.STATUS,
                SEAT.CREATED_AT,
                SEAT.UPDATED_AT
            )
            .values(
                UUID.randomUUID().toString(),
                seat.player.id,
                seat.game.id,
                seat.position.toString(),
                LocalDateTime.now(ZoneOffset.UTC), // Everyone gets one free ping, just to avoid false positive PlayerDisconnected events
                if (seat.player.bot) SeatStatus.BOT.name else SeatStatus.CONNECTED.name,
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC)
            )
            .returningResult(SEAT)
            .fetchOneInto(Seat::class.java)!!
    }

    private fun getPlayers(seats: List<Seat>): List<Player> =
        db.selectFrom(PLAYER)
            .where(PLAYER.ID.`in`(seats.map { it.playerId }))
            .fetchInto(Player::class.java)

    private fun getSeats(game: Game): List<Seat> =
        db.selectFrom(SEAT)
            .where(SEAT.GAME_ID.eq(game.id))
            .fetchInto(Seat::class.java)

    private fun getTricks(handIds: List<Int>): List<Trick> =
        db.selectFrom(TRICK)
            .where(TRICK.HAND_ID.`in`(handIds))
            .orderBy(TRICK.CREATED_AT.desc())
            .fetch(Trick::fromRecord)

    private fun getHands(game: Game): List<Hand> =
        db.selectFrom(HAND)
            .where(HAND.GAME_ID.eq(game.id))
            .orderBy(HAND.CREATED_AT.desc())
            .fetch(Hand::fromRecord)

    private fun rejoinSeat(seat: Seat): Seat {
        // In case the game was set to STALE in the meantime...
        db.update(GAME)
            .set(GAME.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(GAME.STATUS, GameStatus.RUNNING.name)
            .where(GAME.ID.eq(seat.gameId))
            .execute()

        return db.update(SEAT)
            .set(SEAT.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(SEAT.REJOINED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(SEAT.PLAYER_PING, LocalDateTime.now(ZoneOffset.UTC)) // Avoids race condition with playerPing job
            .set(SEAT.STATUS, SeatStatus.CONNECTED.name)
            .where(SEAT.ID.eq(seat.id))
            .returningResult(SEAT)
            .fetchOneInto(Seat::class.java)!!
    }


}