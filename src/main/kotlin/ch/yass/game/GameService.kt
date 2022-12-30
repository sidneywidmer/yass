package ch.yass.game

import arrow.core.Either
import arrow.core.continuations.either
import ch.yass.core.error.DomainError
import ch.yass.game.api.JoinGameResponse
import ch.yass.game.dto.Player
import org.jooq.DSLContext
import java.util.*

class GameService(private val db: DSLContext) {
    fun join(code: String, player: Player): Either<DomainError.DbError, JoinGameResponse> = either.eager {
        // Check game exists

        JoinGameResponse(UUID.randomUUID())
    }
}