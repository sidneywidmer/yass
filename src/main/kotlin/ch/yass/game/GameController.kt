package ch.yass.game

import arrow.core.continuations.either
import ch.yass.core.contract.Controller
import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.successResponse
import ch.yass.core.helper.validate
import ch.yass.game.api.JoinGameRequest
import ch.yass.identity.helper.player
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context

class GameController(private val gameService: GameService) : Controller {
    override val path = "/game"

    override val endpoints = EndpointGroup {
        get("/join", ::join)
    }

    private fun join(ctx: Context) = either.eager {
        val request = validate<JoinGameRequest>(ctx.body()).bind()
        gameService.join(request.code, player(ctx)).bind()
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )
}