package ch.yass.identity

import arrow.core.raise.either
import arrow.core.raise.ensure
import ch.yass.core.contract.Controller
import ch.yass.core.error.InvalidAnonSignup
import ch.yass.core.error.PlayerDoesNotOwnSeat
import ch.yass.core.error.PlayerNotInGame
import ch.yass.core.error.UnauthorizedSubscription
import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.successResponse
import ch.yass.core.helper.validate
import ch.yass.game.GameRepository
import ch.yass.game.PlayerService
import ch.yass.game.api.internal.NewAnonPlayer
import ch.yass.game.engine.playerInGame
import ch.yass.game.engine.playerOwnsSeat
import ch.yass.identity.api.AnonSignupRequest
import ch.yass.identity.api.SubscribeRequest
import ch.yass.identity.api.WhoAmIResponse
import ch.yass.identity.helper.player
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context

class AuthController(private val gameRepo: GameRepository, private val playerService: PlayerService) : Controller {
    override val path = "/auth"

    override val endpoints = EndpointGroup {
        get("/whoami", ::whoami)
        post("/connect", ::connect)
        post("/subscribe", ::subscribe)
        post("/anon/signup", ::anonSignup, EndpointRole.PUBLIC)
    }

    private fun anonSignup(ctx: Context) {
        either {
            // Save anon_token and name here
            // Middlware: if X-Session-Anon-Token is set -> query it to also allow login
            // Godot save anon token
            val request = validate<AnonSignupRequest>(ctx.body())
            playerService.create(NewAnonPlayer(request.name, request.anonToken))
            HIER GEHTS WEITER
            AnonSignupResponse()
        }.fold(
            { errorResponse(ctx, it) },
            { successResponse(ctx, it) }
        )
    }

    private fun whoami(ctx: Context) {
        successResponse(ctx, WhoAmIResponse.from(player(ctx)))
    }

    /**
     * Every new websocket connection to centrifugo gets proxied to this endpoint to ensure authentication. Since
     * the centrifugo server forwards us all client cookies the user has to be authenticated to reach this
     * method (done by AuthMiddleware). We just need to return the users id by the centrifugo spec
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
        either {
            val request = validate<SubscribeRequest>(ctx.body())
            val seatUuid = request.channel.split(":#")[1]  // channel name == seatUuid
            val state = gameRepo.getState(gameRepo.getBySeatUUID(seatUuid))
            val player = player(ctx)

            ensure(playerOwnsSeat(player, seatUuid, state.seats)) { PlayerDoesNotOwnSeat(player, seatUuid, state) }
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