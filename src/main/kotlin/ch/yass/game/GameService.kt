package ch.yass.game

import arrow.core.*
import arrow.core.continuations.either
import ch.yass.core.error.DomainError.*
import ch.yass.db.tables.references.GAME
import ch.yass.db.tables.references.SEAT
import ch.yass.game.api.JoinGameResponse
import ch.yass.game.dto.Player
import ch.yass.game.dto.Game
import ch.yass.game.dto.Seat
import org.jooq.DSLContext
import java.util.*

class GameService(private val db: DSLContext) {
    fun takeASeat(code: String, player: Player): Either<DbError, JoinGameResponse> = either.eager {
        // 1. Get game
        // 2. Get all seats currently occupied in that game
        // 3. Check if there's a free seat (looping positions)
        //      - Break if no free seat
        // 4. Take random seat if any free
        // 5. Return seat
        val game = get(code).bind { Domai }
        val seats = getSeats(game)
        JoinGameResponse(UUID.randomUUID())
//        shift(DomainError.DbError("foo"))
    }

    private fun getSeats(game: Game): Either<DbError, List<Seat>> {
        val seats = db.select()
            .from(SEAT)
            .where(SEAT.GAME_ID.eq(game.id))
            .fetchInto(Seat::class.java)

        return seats.right()
    }

    private fun get(code: String): Option<Game> {
        val game = db.select()
            .from(GAME)
            .where(GAME.CODE.eq(code))
            .fetchOneInto(Game::class.java)

//            ?.right() ?: DomainError.DbError("game.get.empty").left()
        return game.toOption()
    }
}