package ch.yass.identity

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.left
import arrow.core.right
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
            val token = getTokenFromHeader(ctx.headerMap()).bind()
            val session = getSession(oryClient, token).bind()
            val player = playerService.fromSession(session).bind()
            player
        }

        player.tap { }

        when (player) {
            is Either.Left -> throw DomainException(player.value)
            is Either.Right -> ctx.attribute(PLAYER.name, player.value)
        }
    }

    override fun after(ctx: Context) {}

    private fun getTokenFromHeader(headerMap: Map<String, String>): Either<DomainError, String> =
        headerMap["X-Session-Token"]?.right() ?: DomainError.RequestError("header.token.missing").left()

    private fun getSession(oryClient: OryClient, sessionId: String?): Either<DomainError, Session> =
        try {
            oryClient.frontend.toSession(sessionId, null).right()
        } catch (exception: java.lang.Exception) {
            DomainError.OryError("frontend.toSession.exception", exception).left()
        }
}