package ch.yass.game

import arrow.core.raise.either
import ch.yass.core.contract.Controller
import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.logger
import ch.yass.core.helper.successResponse
import ch.yass.core.helper.validate
import ch.yass.game.api.ChooseTrumpRequest
import ch.yass.game.api.GameStateResponse
import ch.yass.game.api.JoinGameRequest
import ch.yass.game.api.PlayCardRequest
import ch.yass.identity.helper.player
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context

class GameController(private val service: GameService) : Controller {
    override val path = "/game"

    override val endpoints = EndpointGroup {
        get("/create", ::create)
        get("/join", ::join)
        get("/play", ::play)
        get("/trump", ::trump)
    }

    // Save new Game to DB
    // Set current player as starting player
    // Create first welcome hand
    private fun create(ctx: Context): Nothing = TODO()

    private fun join(ctx: Context) = either {
        val request = validate<JoinGameRequest>(ctx.body())
        val player = player(ctx)
        val gameState = service.join(request, player)

        logger().info("Player ${player.uuid} joined Game ${gameState.game.uuid}")

        GameStateResponse.from(gameState, player)
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

    private fun play(ctx: Context) = either {
        val request = validate<PlayCardRequest>(ctx.body())
        val player = player(ctx)

        val gameState = service.play(request, player)
        GameStateResponse.from(gameState, player)
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

    private fun trump(ctx: Context) = either {
        val request = validate<ChooseTrumpRequest>(ctx.body())
        val player = player(ctx)

        val gameState = service.trump(request, player)
        GameStateResponse.from(gameState, player)
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )
}