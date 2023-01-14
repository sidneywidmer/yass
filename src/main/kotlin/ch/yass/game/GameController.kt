package ch.yass.game

import arrow.core.continuations.either
import ch.yass.core.contract.Controller
import ch.yass.core.error.DomainError.*
import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.logger
import ch.yass.core.helper.successResponse
import ch.yass.core.helper.validate
import ch.yass.game.api.JoinGameRequest
import ch.yass.game.api.JoinGameResponse
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayCardResponse
import ch.yass.game.api.internal.NewHand
import ch.yass.game.dto.Card
import ch.yass.game.dto.State
import ch.yass.game.engine.*
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
    }

    // Save new Game to DB
    // Set current player as starting player
    // Create first welcome hand
    private fun create(ctx: Context): Nothing = TODO()

    private fun join(ctx: Context) = either.eager {
        val request = validate<JoinGameRequest>(ctx.body()).bind()
        val player = player(ctx)
        val gameState = service.join(request, player)
            .tap { logger().info("Player ${player.uuid} joined Game ${it.game.uuid}") }
            .bind()

        JoinGameResponse.from(gameState, player)
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

    private fun play(ctx: Context) = either.eager {
        val request = validate<PlayCardRequest>(ctx.body()).bind()
        val player = player(ctx)

        service.play(request, player)
            .tap { logger().info("Player ${player.uuid} played Card ${it.card}") }
            .bind()
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )
}