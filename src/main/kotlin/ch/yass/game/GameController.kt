package ch.yass.game

import arrow.core.continuations.either
import ch.yass.core.contract.Controller
import ch.yass.core.error.DomainError.*
import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.successResponse
import ch.yass.core.helper.validate
import ch.yass.game.api.JoinGameRequest
import ch.yass.game.api.JoinGameResponse
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayCardResponse
import ch.yass.game.dto.Card
import ch.yass.game.engine.cardIsPlayable
import ch.yass.game.engine.playerHasTurn
import ch.yass.game.engine.playerOwnsCard
import ch.yass.identity.helper.player
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context

class GameController(private val gameService: GameService) : Controller {
    override val path = "/game"

    override val endpoints = EndpointGroup {
        get("/join", ::join)
        get("/play", ::play)
    }

    private fun join(ctx: Context) = either.eager {
        val request = validate<JoinGameRequest>(ctx.body()).bind()
        val maybeGame = gameService.getByCode(request.code).bind()
        val game = maybeGame.toEither { ValidationError("game.take-a-seat.empty") }.bind()

        gameService.takeASeat(game, player(ctx)).bind()

        val gameState = gameService.getState(game, player(ctx)).bind()

        JoinGameResponse.from(gameState)
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

    private fun play(ctx: Context) = either.eager {
        // x Get request with played card and game (and user)
        // x Get game state
        // x Check if player actually has this card in current trick
        // x Check if it's actually players turn
        // Check if card is valid (either color or trump)
        // Save card to current trick
        // Check if this was the 4th card
        // -> count points / set winning player
        // -> create new trick
        // Check if this was the 9th trick
        // -> create new hand
        // return success
        val request = validate<PlayCardRequest>(ctx.body()).bind()
        val player = player(ctx)
        val game = gameService.getByUUID(request.game).bind()
        val state = gameService.getState(game, player).bind()
        val playedCard = Card.from(request.card)

        ensure(playerOwnsCard(player, playedCard, state)) { ValidationError("card.play.not-owned") }
        ensure(playerHasTurn(player, state)) { ValidationError("card.play.player.locked") }
        ensure(cardIsPlayable(playedCard, player, state)) { ValidationError("card.play.not-playable") }

        PlayCardResponse(true)
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

}