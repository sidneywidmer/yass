package ch.yass.identity

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import ch.yass.core.contract.CtxAttributes.PLAYER
import ch.yass.core.error.DomainError
import ch.yass.core.error.DomainException
import ch.yass.core.contract.Middleware
import ch.yass.game.PlayerService
import io.javalin.http.Context
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
        val player = either.eager {
            val token = getTokenFromHeader(ctx.header("X-Session-Token")).bind()
            val session = getSession(oryClient, token).bind()
            val player = playerService.playerFromSession(session).bind()
            player
        }

        when (player) {
            is Either.Left -> throw DomainException(player.value)
            is Either.Right -> ctx.attribute(PLAYER.name, player.value)
        }
    }

    override fun after(ctx: Context) {}

    private fun getTokenFromHeader(token: String?): Either<DomainError, String> =
        token?.right() ?: DomainError.RequestError("header.token.missing").left()


    private fun getSession(oryClient: OryClient, sessionId: String?): Either<DomainError, Session> =
        try {
            oryClient.frontend.toSession(sessionId, null).right()
        } catch (exception: java.lang.Exception) {
            // TODO: Can we remove the stack trace? We don't need it...
            DomainError.OryError("frontend.toSession.exception", exception).left()
        }
}