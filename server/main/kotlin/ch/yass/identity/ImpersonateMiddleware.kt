package ch.yass.identity

import arrow.core.raise.either
import ch.yass.core.contract.CtxAttributes
import ch.yass.core.contract.Middleware
import ch.yass.core.error.CanNotImpersonate
import ch.yass.core.helper.logger
import ch.yass.core.helper.toUUID
import ch.yass.core.helper.validate
import ch.yass.game.PlayerService
import ch.yass.identity.api.ImpersonateRequest
import ch.yass.identity.helper.isAdmin
import ch.yass.identity.helper.player
import io.javalin.http.Context
import io.javalin.http.HandlerType

/**
 * If we find a request with a body that contains the json field data.impersonate and the current
 * user is an admin we overwrite the PLAYER on the ctx to the new impersonated player.
 */
class ImpersonateMiddleware(
    private val playerService: PlayerService
) : Middleware {
    override fun before(ctx: Context) {
        // See AuthMiddleware
        if (ctx.method() == HandlerType.OPTIONS) {
            return
        }

        val player = player(ctx)
        if (!isAdmin(player)) {
            return
        }

        either {
            val request = validate<ImpersonateRequest>(ctx.body())
            val uuid = request.data.impersonate.toUUID()
            playerService.get(uuid) ?: raise(CanNotImpersonate(player, uuid))
        }.onRight {
            logger().info("Player ${player.uuid} impersonating ${it.uuid} for request ${ctx.path()}")
            ctx.attribute(CtxAttributes.PLAYER.name, it)
            ctx.attribute(CtxAttributes.BEFORE_IMPERSONATION.name, player)
        }
    }

    override fun after(ctx: Context) {}
}