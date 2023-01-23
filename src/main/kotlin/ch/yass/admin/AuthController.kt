package ch.yass.admin

import ch.yass.admin.api.GenerateHandResponse
import ch.yass.core.contract.Controller
import ch.yass.core.helper.successResponse
import ch.yass.game.dto.Position
import ch.yass.game.engine.cardToNotation
import ch.yass.game.engine.randomHand
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context


class AdminController : Controller {
    override val path = "/admin"

    override val endpoints = EndpointGroup {
        get("/generate/hand", ::generateHand)
    }

    private fun generateHand(ctx: Context) {
        val hand = randomHand()
        successResponse(
            ctx, GenerateHandResponse(
                north = hand[Position.NORTH]!!.joinToString(",") { cardToNotation(it) },
                east = hand[Position.EAST]!!.joinToString(",") { cardToNotation(it) },
                south = hand[Position.SOUTH]!!.joinToString(",") { cardToNotation(it) },
                west = hand[Position.WEST]!!.joinToString(",") { cardToNotation(it) },
            )
        )
    }
}