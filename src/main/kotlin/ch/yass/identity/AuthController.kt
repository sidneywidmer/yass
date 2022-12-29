package ch.yass.identity

import ch.yass.core.contract.Controller
import ch.yass.core.contract.CtxAttributes
import ch.yass.game.dto.Player
import ch.yass.identity.api.WhoAmI
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.Config
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context


class AuthController(private val config: Config, private val mapper: ObjectMapper) : Controller {
    override val path = "/auth"

    override val endpoints = EndpointGroup {
        get("/whoami", ::whoami)
    }

    private fun whoami(ctx: Context) {
        val player = ctx.attribute<Player>(CtxAttributes.PLAYER.name)

        ctx.json(mapper.writeValueAsString(WhoAmI.from(player!!)))
    }
}