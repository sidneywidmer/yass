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
import ch.yass.game.api.internal.NewHand
import ch.yass.game.dto.Card
import ch.yass.game.dto.State
import ch.yass.game.engine.*
import ch.yass.identity.helper.player
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context

class GameController(private val gameService: GameService) : Controller {
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
        val maybeGame = gameService.getByCode(request.code).bind()
        val game = maybeGame.toEither { ValidationError("game.take-a-seat.empty") }.bind()
        val player = player(ctx)

        gameService.takeASeat(game, player).bind()

        val gameState = gameService.getState(game).bind()

        JoinGameResponse.from(gameState, player)
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

    private fun play(ctx: Context) = either.eager {
        val request = validate<PlayCardRequest>(ctx.body()).bind()
        val player = player(ctx)
        val game = gameService.getByUUID(request.game).bind()
        val state = gameService.getState(game).bind()
        val playedCard = Card.from(request.card)

        ensure(playerHasTurn(player, state)) { ValidationError("card.play.player.locked") }
        ensure(playerOwnsCard(player, playedCard, state)) { ValidationError("card.play.not-owned") }
        ensure(cardIsPlayable(playedCard, player, state)) { ValidationError("card.play.not-playable") }

        gameService.playCard(playedCard, currentTrick(state), playerSeat(player, state)).bind()

        val updatedState = gameService.getState(game).bind()
        when (nextState(updatedState)) {
            State.PLAY_CARD -> PlayCardResponse(true)
            State.NEW_TRICK -> gameService.createTrick(currentHand(state)).bind()
            State.NEW_HAND -> {
                val startingPlayer = nextTrickStartingPlayer(updatedState)
                val newHand = gameService.createHand(NewHand(game, startingPlayer, randomHand())).bind()
                gameService.createTrick(newHand).bind()
            }

            else -> shift(UnexpectedError("game.state.invalid"))
        }
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )
}