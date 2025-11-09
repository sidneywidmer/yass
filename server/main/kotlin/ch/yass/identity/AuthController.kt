package ch.yass.identity

import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import ch.yass.core.contract.Controller
import ch.yass.core.error.*
import ch.yass.core.helper.createToken
import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.hashToken
import ch.yass.core.helper.logger
import ch.yass.core.helper.successResponse
import ch.yass.core.helper.toUUID
import ch.yass.core.helper.validate
import ch.yass.game.GameRepository
import ch.yass.game.PlayerService
import ch.yass.game.api.internal.NewAnonPlayer
import ch.yass.game.dto.db.InternalPlayer
import ch.yass.game.engine.playerInGame
import ch.yass.game.engine.playerOwnsSeat
import ch.yass.identity.api.*
import ch.yass.identity.helper.player
import com.typesafe.config.Config
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context
import io.javalin.http.Cookie
import io.javalin.http.SameSite
import sh.ory.ApiException
import sh.ory.model.Session

class AuthController(
    private val gameRepo: GameRepository,
    private val playerService: PlayerService,
    private val oryClient: OryClient,
    private val config: Config,
) : Controller {
    override val path = "/auth"

    override val endpoints = EndpointGroup {
        get("/whoami", ::whoami)
        post("/connect", ::connect)
        post("/subscribe", ::subscribe)
        post("/anon/signup", ::anonSignup, EndpointRole.PUBLIC)
        get("/anon/logout", ::anonLogout)
        post("/anon/link", ::anonLink)
    }

    /**
     * Anonymous users don't have an ory_uuid in the DB but an anon_token. When an anon user links his
     * profile with an ory account, we can delete the anon_token and set the ory_uuid.
     */
    private fun anonLink(ctx: Context) {
        either {
            val request = validate<AnonLinkRequest>(ctx.body())
            val player = player(ctx)

            ensureNotNull(player.anonToken) { CanNotLinkAnonAccount(player, request.orySession) }

            val orySession = getSession(player, request.orySession)
            playerService.linkAnonAccount(player, orySession.identity!!.id.toUUID(), request.orySession)

            object {
                val success = true
            }
        }.fold(
            { errorResponse(ctx, it) },
            { successResponse(ctx, it) }
        )
    }

    private fun anonSignup(ctx: Context) {
        either {
            val request = validate<AnonSignupRequest>(ctx.body())
            val token = createToken()
            val player = playerService.create(NewAnonPlayer(request.name, hashToken(token)))

            ctx.cookie(
                Cookie("anon_token", token).apply {
                    isHttpOnly = true
                    path = "/"
                    sameSite = SameSite.NONE
                    secure = true
                    domain = config.getString("server.cookieDomain")
                    maxAge = 60 * 60 * 24 * 365 // 1 year
                }
            )
            logger().info("trigger_alert: New guest user signed up ${player.name} (${player.uuid})")
            AnonSignupResponse(player.uuid, player.name)
        }.fold(
            { errorResponse(ctx, it) },
            { successResponse(ctx, it) }
        )
    }

    private fun anonLogout(ctx: Context) {
        playerService.resetAnonToken(player(ctx))
        ctx.cookie(
            Cookie("anon_token", "").apply {
                isHttpOnly = true
                path = "/"
                sameSite = SameSite.NONE
                secure = true
                domain = config.getString("server.cookieDomain")
                maxAge = 0 // Delete cookie by setting maxAge to 0
            }
        )
        successResponse(ctx, object {
            val result = object {}
        })
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

    context(Raise<CanNotLinkAnonAccount>)
    private fun getSession(player: InternalPlayer, orySession: String): Session =
        arrow.core.raise.catch({
            oryClient.frontend.toSession(orySession, null, null)
        }) { _: ApiException -> raise(CanNotLinkAnonAccount(player, orySession)) }

}