package ch.yass.game

import arrow.core.Either
import arrow.core.continuations.either
import ch.yass.core.contract.Controller
import ch.yass.core.contract.CtxAttributes
import ch.yass.core.error.DomainException
import ch.yass.core.helper.response
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

    private fun join(ctx: Context) {
        // - [ ] Validate user and game
        // - [ ] Add user to game in db if there's still free space
        // - [ ] Return: Game uuid, seat position, all other players and their eventually played
        //          card in this trick, own hand of cards
        validate<JoinGameRequest>(ctx.body()).fold(
            { error -> response(ctx, error) },
            { request -> response(ctx, gameService.join(request.code, player(ctx))) }
        )
    }
}