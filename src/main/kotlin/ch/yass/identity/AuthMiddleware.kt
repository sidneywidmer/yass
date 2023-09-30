package ch.yass.identity

import arrow.core.Either
import arrow.core.raise.Raise
import ch.yass.core.error.SessionTokenMissing
import ch.yass.core.contract.CtxAttributes.PLAYER
import ch.yass.core.error.DomainException
import ch.yass.core.contract.Middleware
import ch.yass.game.PlayerService
import io.javalin.http.Context
import sh.ory.model.Session
import arrow.core.raise.*

/**
 * Every request to the yass-server needs to be somehow authenticated. The whole identity
 * management is provided by ory.
 */
class AuthMiddleware(
    private val oryClient: OryClient,
    private val playerService: PlayerService
) : Middleware {
    override fun before(ctx: Context) {
        val maybePlayer = either {
            val token = getTokenFromHeader(ctx.headerMap())
            val session = getSession(oryClient, token)
            val player = playerService.fromSession(session)
            player
        }

        when (maybePlayer) {
            is Either.Left -> throw DomainException(maybePlayer.value)
            is Either.Right -> ctx.attribute(PLAYER.name, maybePlayer.value)
        }
    }

    override fun after(ctx: Context) {}

    context(Raise<SessionTokenMissing>)
    private fun getTokenFromHeader(headerMap: Map<String, String>): String =
        headerMap["X-Session-Token"] ?: raise(SessionTokenMissing)

    private fun getSession(oryClient: OryClient, sessionId: String?): Session =
        oryClient.frontend.toSession(sessionId, null)
}