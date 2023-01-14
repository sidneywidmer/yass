package ch.yass.game

import arrow.core.*
import arrow.core.continuations.either
import ch.yass.core.error.DomainError
import ch.yass.core.error.DomainError.*
import ch.yass.core.helper.toDbJson
import ch.yass.db.tables.references.*
import ch.yass.game.api.internal.GameState
import ch.yass.game.api.internal.NewHand
import ch.yass.game.api.internal.NewSeat
import ch.yass.game.dto.*
import ch.yass.game.dto.db.*
import ch.yass.game.dto.db.Game
import ch.yass.game.engine.*
import org.jooq.DSLContext
import org.jooq.Records.mapping
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class GameRepository(private val db: DSLContext) {
    fun takeASeat(game: Game, player: Player): Either<DomainError, Seat> = either.eager {
        val seats = getSeats(game).bind()
        val maybeAlreadySeated = seats.firstOrNull { it.playerId == player.id }.toOption()
        maybeAlreadySeated.fold(
            {
                val position = freePosition(seats).bind()
                createSeat(NewSeat(game, player, position)).bind()
            },
            { rejoinSeat(it).bind() }
        )
    }

    /**
     * Collect all the info we have about a game and pass it to the engine which is responsible
     * to interpret the given data.
     */
    fun getState(game: Game): Either<DomainError, GameState> = either.eager {
        val seats = getSeats(game).bind()

        // Currently the first player to join a room is also starting the game, maybe randomize?
        // I also don't like that we pass the player just to create the first hand...
        val hands = getHands(game).bind()
        val tricks = getTricks(hands.map { it.id }).bind()
        val players = getPlayers(seats).bind()

        GameState(game, players, seats, hands, tricks)
    }

    fun getByCode(code: String): Either<DbError, Option<Game>> = Either.catch {
        db.selectFrom(GAME)
            .where(GAME.CODE.eq(code))
            .fetchOneInto(Game::class.java)
            .toOption()
    }.mapLeft { DbError("game.get-by-code.error", it) }

    fun getByUUID(uuid: String): Either<DomainError, Game> = Either.catch {
        val game = db.selectFrom(GAME)
            .where(GAME.UUID.eq(uuid))
            .fetchOneInto(Game::class.java)

        return game?.right() ?: ValidationError("game.get-by-uuid.uuid.invalid").left()
    }.mapLeft { DbError("game.get-by-uuid.error", it) }

    private fun getPlayers(seats: List<Seat>): Either<DbError, List<Player>> = Either.catch {
        db.selectFrom(PLAYER)
            .where(PLAYER.ID.`in`(seats.map { it.playerId }))
            .fetchInto(Player::class.java)
    }.mapLeft { DbError("players.get.error", it) }

    private fun getSeats(game: Game): Either<DbError, List<Seat>> = Either.catch {
        db.selectFrom(SEAT)
            .where(SEAT.GAME_ID.eq(game.id))
            .fetchInto(Seat::class.java)
    }.mapLeft { DbError("seats.get.error", it) }

    private fun getTricks(handIds: List<Int>): Either<DbError, List<Trick>> = Either.catch {
        db.selectFrom(TRICK)
            .where(TRICK.HAND_ID.`in`(handIds))
            .orderBy(TRICK.CREATED_AT.desc())
            .fetch(Trick::fromRecord)
    }.mapLeft { DbError("tricks.get.error", it) }

    private fun getHands(game: Game): Either<DbError, List<Hand>> = Either.catch {
        db.selectFrom(HAND)
            .where(HAND.GAME_ID.eq(game.id))
            .orderBy(HAND.CREATED_AT.desc())
            .fetch(Hand::fromRecord)
    }.mapLeft { DbError("hand.get.error", it) }

    fun createHand(hand: NewHand): Either<DbError, Hand> = Either.catch {
        val createdHand = db
            .insertInto(
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
                HAND.WEST
            )
            .values(
                UUID.randomUUID().toString(),
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
                hand.game.id,
                hand.startingPlayer.id,
                null,
                false,
                0,
                toDbJson(hand.positions[Position.NORTH]),
                toDbJson(hand.positions[Position.EAST]),
                toDbJson(hand.positions[Position.SOUTH]),
                toDbJson(hand.positions[Position.WEST]),
            )
            .returningResult(HAND)
            .fetchOne(mapping(Hand::fromRecord))

        return createdHand?.right() ?: DbError("hand.create.empty").left()
    }.mapLeft { DbError("hand.create.error", it) }

    private fun createSeat(seat: NewSeat): Either<DbError, Seat> = Either.catch {
        val createdSeat = db
            .insertInto(SEAT, SEAT.UUID, SEAT.PLAYER_ID, SEAT.GAME_ID, SEAT.POSITION, SEAT.CREATED_AT, SEAT.UPDATED_AT)
            .values(
                UUID.randomUUID().toString(),
                seat.player.id,
                seat.game.id,
                seat.position.toString(),
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC)
            )
            .returningResult(SEAT)
            .fetchOneInto(Seat::class.java)

        return createdSeat?.right() ?: DbError("seat.create.empty").left()
    }.mapLeft { DbError("seat.create.error", it) }

    fun createTrick(hand: Hand): Either<DbError, Trick> = Either.catch {
        val createdTrick = db
            .insertInto(TRICK, TRICK.UUID, TRICK.CREATED_AT, TRICK.UPDATED_AT, TRICK.POINTS, TRICK.HAND_ID)
            .values(
                UUID.randomUUID().toString(),
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC),
                0,
                hand.id
            )
            .returningResult(TRICK)
            .fetchOneInto(Trick::class.java)

        return createdTrick?.right() ?: DbError("trick.create.empty").left()
    }.mapLeft { DbError("trick.create.error", it) }

    private fun rejoinSeat(seat: Seat): Either<DbError, Seat> = Either.catch {
        val updatedSeat = db.update(SEAT)
            .set(SEAT.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(SEAT.REJOINED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .where(SEAT.ID.eq(seat.id))
            .returningResult(SEAT)
            .fetchOneInto(Seat::class.java)

        return updatedSeat?.right() ?: DbError("seat.create.empty").left()
    }.mapLeft { DbError("seat.rejoin.error", it) }

    fun playCard(card: Card, trick: Trick, seat: Seat): Either<DbError, Trick> = Either.catch {
        val updatedTrick = db.update(TRICK)
            .set(TRICK.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(seat.trickColumn(), toDbJson(card))
            .where(TRICK.ID.eq(trick.id))
            .returningResult(TRICK)
            .fetchOneInto(Trick::class.java)

        return updatedTrick?.right() ?: DbError("trick.play-card.empty").left()
    }.mapLeft { DbError("trick.play-card.error", it) }
}