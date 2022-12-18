package ch.yass.identity

import arrow.core.Either
import arrow.core.Either.Companion.catch
import arrow.core.Option
import arrow.core.continuations.either
import arrow.core.continuations.either.eager
import arrow.core.continuations.nullable
import arrow.core.flatMap
import ch.yass.core.CtxAttributes.PLAYER
import ch.yass.core.DomainError
import ch.yass.core.contract.Middleware
import ch.yass.game.PlayerService
import ch.yass.game.dto.Player
import io.javalin.http.Context
import io.javalin.http.ForbiddenResponse
import sh.ory.model.Session

/**
 * Every request to the yass-server needs to be somehow authenticated. The whole identity
 * management is provided by ory.
 */
class AuthMiddleware(
    private val oryClient: OryClient,
    private val playerService: PlayerService
) : Middleware {
    override fun before(ctx: Context) {
        val player = eager {
            val token = getTokenFromHeader(ctx).bind()
            val session = getSession(token).bind()
            playerService.playerFromSession(session).bind()
        }

        when (player) {
            is Either.Left -> when (player.value) {
                is DomainError.RequestError -> throw ForbiddenResponse("Session-Token header missing.")
                is DomainError.OryError -> throw ForbiddenResponse("Invalid session token.")
                else -> throw ForbiddenResponse("Nope.")
            }
            is Either.Right -> ctx.attribute(PLAYER.name, player.value)
        }
    }

    override fun after(ctx: Context) {}

    private fun getTokenFromHeader(ctx: Context): Either<DomainError, String> {
        return Option.fromNullable(ctx.header("X-Session-Token"))
            .toEither { DomainError.RequestError("token.missing") }
    }

    private fun getSession(sessionId: String?): Either<DomainError, Session> = catch {
        oryClient.frontend.toSession(sessionId, null)
    }.mapLeft { throwable: Throwable ->
        DomainError.OryError("frontend.toSession", throwable)
    }
}