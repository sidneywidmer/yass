package ch.yass.identity

import ch.yass.core.contract.Controller
import ch.yass.core.helper.successResponse
import ch.yass.identity.api.WhoAmIResponse
import ch.yass.identity.helper.player
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
        successResponse(ctx, WhoAmIResponse.from(player(ctx)))
    }
}