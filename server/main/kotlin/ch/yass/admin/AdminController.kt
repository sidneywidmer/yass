package ch.yass.admin

import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.recover
import ch.yass.admin.api.GenerateHandResponse
import ch.yass.admin.dsl.game
import ch.yass.core.contract.Controller
import ch.yass.core.error.PlayerNotInGame
import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.successResponse
import ch.yass.game.GameService
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.dto.Card
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.cardToNotation
import ch.yass.game.engine.nextState
import ch.yass.game.engine.playerAtPosition
import ch.yass.game.engine.playerInGame
import ch.yass.game.engine.randomHand
import ch.yass.identity.EndpointRole
import ch.yass.identity.helper.player
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context


class AdminController(
    private val analyzeGameService: AnalyzeGameService,
) :
    Controller {
    override val path = "/admin"

    override val endpoints = EndpointGroup {
        get("/ping", ::ping, EndpointRole.PUBLIC)
        get("/analyze/game/{code}", ::analyzeGame)
    }

    private fun ping(ctx: Context) {
        successResponse(ctx, object {
            val pong = true
        })
    }

    private fun analyzeGame(ctx: Context) = either {
        analyzeGameService.analyze(ctx.pathParam("code"), player(ctx))
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )
}