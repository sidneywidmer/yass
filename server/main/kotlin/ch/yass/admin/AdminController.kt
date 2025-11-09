package ch.yass.admin

import arrow.core.raise.either
import ch.yass.admin.api.MessageRequest
import ch.yass.admin.api.internal.CreateMessage
import ch.yass.core.contract.Controller
import ch.yass.core.helper.config
import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.successResponse
import ch.yass.core.helper.validate
import ch.yass.identity.EndpointRole
import ch.yass.identity.helper.player
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context

class AdminController(
    private val analyzeGameService: AnalyzeGameService,
    private val messageService: MessageService,
) : Controller {
    override val path = "/admin"

    override val endpoints = EndpointGroup {
        get("/ping", ::ping, EndpointRole.PUBLIC)
        get("/analyze/game/{code}", ::analyzeGame)
        post("/message", ::message)
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

    private fun message(ctx: Context) = either {
        val request = validate<MessageRequest>(ctx.body())
        val player = player(ctx)
        messageService.createMessage(
            CreateMessage(
                player,
                request.gameUuid,
                request.message,
                request.path,
                config().getString("serverVersion")
            )
        )
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )
}