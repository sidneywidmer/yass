package ch.yass.identity

import arrow.core.raise.either
import arrow.core.raise.ensure
import ch.yass.core.contract.Controller
import ch.yass.core.error.PlayerNotInGame
import ch.yass.core.error.UnauthorizedSubscription
import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.successResponse
import ch.yass.core.helper.validate
import ch.yass.game.GameRepository
import ch.yass.game.engine.playerInGame
import ch.yass.identity.api.SubscribeRequest
import ch.yass.identity.api.WhoAmIResponse
import ch.yass.identity.helper.player
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context

class AuthController(private val repo: GameRepository) : Controller {
    override val path = "/auth"

    override val endpoints = EndpointGroup {
        get("/whoami", ::whoami)
        post("/connect", ::connect)
        post("/subscribe", ::subscribe)
    }

    private fun whoami(ctx: Context) {
        successResponse(ctx, WhoAmIResponse.from(player(ctx)))
    }

    /**
     * Every new websocket connection to centrifugo gets proxied to this endpoint to ensure authentication. Since
     * the centrifugo server forwards us all client cookies the user has to be authenticated to reach this
     * method * (done by the AuthMiddleware). We just need to return the users id by the centrifugo spec
     * for a successful connection.
     */
    private fun connect(ctx: Context) {
        successResponse(ctx, object {
            val result = object {
                val user = player(ctx).uuid
            }
        })
    }

    /**
     * Whenever a centrifugo client tries to subscribe to a channel this endpoint is called, and we'll verify that
     * the user has access to this game.
     */
    private fun subscribe(ctx: Context) {
        Hier gehts weiter -> admin user können allen games joinen
        either {
            val request = validate<SubscribeRequest>(ctx.body())
            val game = repo.getByUUID(request.channel.split(":#")[1]) // channel name == gameUuid
            val state = repo.getState(game)
            val player = player(ctx)
            ensure(playerInGame(player, state.seats)) { PlayerNotInGame(player, state) }
        }.fold(
            { errorResponse(ctx, UnauthorizedSubscription(it)) },
            {
                successResponse(ctx, object {
                    val result = object {}
                })
            }
        )
    }
}