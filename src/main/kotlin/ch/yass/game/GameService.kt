package ch.yass.game

import arrow.core.*
import arrow.core.continuations.either
import ch.yass.core.error.DomainError
import ch.yass.core.error.DomainError.*
import ch.yass.core.helper.toDbJson
import ch.yass.db.tables.references.GAME
import ch.yass.db.tables.references.HAND
import ch.yass.db.tables.references.SEAT
import ch.yass.game.api.GameState
import ch.yass.game.api.GameStatePlayer
import ch.yass.game.api.internal.NewHand
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Seat
import org.jooq.DSLContext
import org.jooq.Records.mapping
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class GameService(private val db: DSLContext) {
    fun takeASeat(game: Game, player: Player): Either<DomainError, Seat> = either.eager {
        val seats = getSeats(game).bind()
        val maybeAlreadySeated = seats.firstOrNull { it.playerId == player.id }.toOption()
        maybeAlreadySeated.fold(
            {
                val position = getFreePosition(seats).bind()
                createSeat(player, game, position).bind()
            },
            { rejoinSeat(it).bind() }
        )
    }

    fun createSeat(player: Player, game: Game, position: Position): Either<DbError, Seat> = Either.catch {
        val createdSeat = db
            .insertInto(SEAT, SEAT.UUID, SEAT.PLAYER_ID, SEAT.GAME_ID, SEAT.POSITION, SEAT.CREATED_AT, SEAT.UPDATED_AT)
            .values(
                UUID.randomUUID().toString(),
                player.id,
                game.id,
                position.toString(),
                LocalDateTime.now(ZoneOffset.UTC),
                LocalDateTime.now(ZoneOffset.UTC)
            )
            .returningResult(SEAT)
            .fetchOneInto(Seat::class.java)

        return createdSeat?.right() ?: DbError("seat.create.empty").left()
    }.mapLeft { DbError("seat.create.error", it) }

    fun getSeats(game: Game): Either<DbError, List<Seat>> = Either.catch {
        db.selectFrom(SEAT)
            .where(SEAT.GAME_ID.eq(game.id))
            .fetchInto(Seat::class.java)
    }.mapLeft { DbError("seats.get.error", it) }

    fun get(code: String): Either<DbError, Option<Game>> = Either.catch {
        db.selectFrom(GAME)
            .where(GAME.CODE.eq(code))
            .fetchOneInto(Game::class.java)
            .toOption()
    }.mapLeft { DbError("game.get.error", it) }

    fun getState(game: Game, player: Player): Either<DomainError, GameState> = either.eager {
        val seats = getSeats(game).bind()
        val playerSeat = seats.first { it.playerId == player.id }

        val maybeHand = getCurrentHand(game).bind()
        val hand: Hand = maybeHand.fold(
            {
                val positions = mapOf(
                    Position.NORTH to listOf(Card(Suit.CLUBS, Rank.ACE, "french")),
                    Position.EAST to listOf(Card(Suit.CLUBS, Rank.ACE, "french")),
                    Position.SOUTH to listOf(Card(Suit.CLUBS, Rank.ACE, "french")),
                    Position.WEST to listOf(Card(Suit.CLUBS, Rank.ACE, "french")),
                )
                createHand(NewHand(game, player, positions)).bind()
            },
            { it }
        )

        val trick = getCurrentTrick().bind()

        // get the latest hand of game -> if not exists create with `player` as staring player
        // based on `player` position -> get this players hand
        // get all other players currently sitting at the game table
        // get the latest trick of this hand to indicate what was already played -> map to GameStatePlayer (other players -> played card)
        GameState(
            game.uuid,
            listOf(GameStatePlayer("Hansi")),
            hand.trump,
            hand.cardsOf(playerSeat.position),
            playerSeat.position,
            playerSeat.rejoinedAt
        )
    }

    private fun createHand(hand: NewHand): Either<DbError, Hand> = Either.catch {
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

    private fun getCurrentHand(game: Game): Either<DbError, Option<Hand>> = Either.catch {
        db.selectFrom(HAND)
            .where(HAND.GAME_ID.eq(game.id))
            .orderBy(HAND.CREATED_AT.desc())
            .fetchOne(Hand::fromRecord)
            .toOption()
    }.mapLeft { DbError("hand.get-current.error", it) }


    private fun getFreePosition(occupiedSeats: List<Seat>): Either<ValidationError, Position> {
        val occupied = occupiedSeats.map { it.position }.toSet()
        val all = Position.values().toSet()
        val maybePosition = all
            .minus(occupied)
            .randomOrNull()

        return maybePosition?.right() ?: ValidationError("game.take-a-seat.full").left()
    }

    private fun rejoinSeat(seat: Seat): Either<DbError, Seat> = Either.catch {
        val updatedSeat = db.update(SEAT)
            .set(SEAT.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .set(SEAT.REJOINED_AT, LocalDateTime.now(ZoneOffset.UTC))
            .where(SEAT.ID.eq(seat.id))
            .returningResult(SEAT)
            .fetchOneInto(Seat::class.java)

        return updatedSeat?.right() ?: DbError("seat.create.empty").left()
    }.mapLeft { DbError("seat.rejoin.error", it) }
}