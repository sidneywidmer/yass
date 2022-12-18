package ch.yass.identity

import ch.yass.core.CtxAttributes
import ch.yass.core.contract.Controller
import ch.yass.game.dto.Player
import com.typesafe.config.Config
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context
import sh.ory.model.Session

class AuthController(private val config: Config) : Controller {
    override val path = "/auth"

    override val endpoints = EndpointGroup {
        get("/whoami", ::whoami)
    }

    private fun whoami(ctx: Context) {
        ctx.json(object {
            val player = ctx.attribute<Player>(CtxAttributes.PLAYER.name)
        })
    }
}