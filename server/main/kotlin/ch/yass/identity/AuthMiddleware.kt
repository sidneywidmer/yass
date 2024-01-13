package ch.yass.identity

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import ch.yass.core.contract.CtxAttributes.PLAYER
import ch.yass.core.contract.Middleware
import ch.yass.core.error.DomainException
import ch.yass.core.error.Unauthorized
import ch.yass.game.PlayerService
import io.javalin.http.Context
import io.javalin.http.HandlerType.OPTIONS
import sh.ory.ApiException
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
        if (ctx.routeRoles().contains(EndpointRole.PUBLIC)) {
            return
        }

        val maybePlayer = either {
            val session = getSession(
                oryClient,
                ctx.headerMap()["X-Session-Token"],
                ctx.cookieMap()["ory_kratos_session"]
            )
            val player = playerService.fromSession(session)

            player
        }


        when (maybePlayer) {
            is Either.Left -> throw DomainException(maybePlayer.value)
            is Either.Right -> ctx.attribute(PLAYER.name, maybePlayer.value)
        }
    }

    override fun after(ctx: Context) {}

    context(Raise<Unauthorized>)
    private fun getSession(oryClient: OryClient, session: String?, cookie: String?): Session =
        catch({
            oryClient.frontend.toSession(session, cookie?.let { "ory_kratos_session=$cookie" })
        }) { e: ApiException -> raise(Unauthorized(e)) }
}