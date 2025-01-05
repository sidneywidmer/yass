package ch.yass.identity

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import ch.yass.core.contract.CtxAttributes.PLAYER
import ch.yass.core.contract.Middleware
import ch.yass.core.error.DomainException
import ch.yass.core.error.InvalidAnonToken
import ch.yass.core.error.Unauthorized
import ch.yass.game.PlayerService
import io.javalin.http.Context
import sh.ory.ApiException
import sh.ory.model.Session

/**
 * Every request to the yass-server needs to be somehow authenticated. The whole identity management is provided by ory.
 * We have a really simple system in place for anonymous users. When "signing up" as anon user, the game client
 * just saves a token on the device which is also persisted on the server and sent for every request.
 */
class AuthMiddleware(
    private val oryClient: OryClient,
    private val playerService: PlayerService
) : Middleware {
    override fun before(ctx: Context) {
        if (ctx.routeRoles().contains(EndpointRole.PUBLIC)) {
            return
        }

        val anonToken = ctx.cookieMap()["anon_token"]
        val maybePlayer = either {
            if (anonToken != null) {
                ensureNotNull(playerService.getByAnonToken(anonToken)) { InvalidAnonToken(anonToken) }
            } else {
                val session = getSession(ctx.cookieMap()["ory_kratos_session"])
                playerService.fromSession(session)
            }
        }

        when (maybePlayer) {
            is Either.Left -> throw DomainException(maybePlayer.value)
            is Either.Right -> ctx.attribute(PLAYER.name, maybePlayer.value)
        }
    }

    context(Raise<Unauthorized>)
    private fun getSession(cookie: String?): Session =
        catch({
            oryClient.frontend.toSession(null, cookie?.let { "ory_kratos_session=$cookie" }, null)
        }) { e: ApiException -> raise(Unauthorized(e)) }
}