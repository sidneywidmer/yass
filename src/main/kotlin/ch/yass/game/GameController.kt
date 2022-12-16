package ch.yass.game

import ch.yass.config.contract.Controller
import ch.yass.utils.logger
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context

class GameController : Controller {
    override val path = "/game"

    override val endpoints = EndpointGroup {
        get("/join", ::join)
    }

    private fun join(ctx: Context) {
        // - [ ] Validate user and game
        // - [ ] Add user to game in db if there's still free space
        // - [ ] Return: Game uuid, seat position, all other players and their eventually played
        //          card in this trick, own hand of cards
        logger().info("bar")

        ctx.json(object {
            val data = "Pong"
        })
    }
}