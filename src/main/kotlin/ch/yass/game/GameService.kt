package ch.yass.game

import arrow.core.*
import arrow.core.continuations.either
import ch.yass.core.error.DomainError
import ch.yass.core.error.DomainError.*
import ch.yass.core.helper.toUUID
import ch.yass.db.tables.references.GAME
import ch.yass.db.tables.references.SEAT
import ch.yass.game.api.JoinGameResponse
import ch.yass.game.dto.Player
import ch.yass.game.dto.Game
import ch.yass.game.dto.Position
import ch.yass.game.dto.Seat
import org.jooq.DSLContext
import org.jooq.Records.mapping
import org.jooq.impl.DSL.row
import java.util.*

class GameService(private val db: DSLContext) {
    fun takeASeat(code: String, player: Player): Either<DomainError, List<Seat>> = either.eager {
        // 3. Check if there's a free seat (looping positions)
        //      - Break if no free seat
        // 4. Take random seat if any free
        // 5. Return seat
        val maybeGame = get(code).bind()
        val game = maybeGame.toEither { ValidationError("game.take-a-seat.empty") }.bind()
        val seats = getSeats(game).bind()
        val position = getFreePosition(seats).bind()

        seats
    }

    private fun getFreePosition(seats: List<Seat>): Either<ValidationError, Position> {
        val occupied = seats.map { it.position }.toSet()
        val free = Position.values().toSet()
        val maybePosition = free
            .minus(occupied)
            .randomOrNull()

        return maybePosition?.right() ?: ValidationError("game.take-a-seat.full").left()
    }

    private fun getSeats(game: Game): Either<DbError, List<Seat>> = Either.catch {
        db.select(
            SEAT,
            row(SEAT.player()).mapping { it.into(Player::class.java) })
            .from(SEAT)
            .where(SEAT.GAME_ID.eq(game.id))
            .fetch(mapping { seat, player ->
                Seat(
                    seat.id!!,
                    UUID.fromString(seat.uuid!!),
                    seat.createdAt!!,
                    seat.updatedAt!!,
                    player!!,
                    game,
                    Position.valueOf(seat.position!!)
                )
            })
    }.mapLeft { DbError("seats.get.error", it) }

    private fun get(code: String): Either<DbError, Option<Game>> = Either.catch {
        db.select()
            .from(GAME)
            .where(GAME.CODE.eq(code))
            .fetchOneInto(Game::class.java)
            .toOption()
    }.mapLeft { DbError("game.get.error", it) }
}