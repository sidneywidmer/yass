package ch.yass.game

import arrow.core.raise.either
import ch.yass.admin.api.PlayGameResponse
import ch.yass.core.contract.Controller
import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.successResponse
import ch.yass.core.helper.validate
import ch.yass.game.api.*
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.SeatState
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.*
import ch.yass.identity.helper.player
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context

class GameController(private val service: GameService) : Controller {
    override val path = "/game"

    override val endpoints = EndpointGroup {
        post("/create", ::create)
        post("/join", ::join)
        post("/play", ::play)
        post("/trump", ::trump)
        post("/schiebe", ::schiebe)
    }

    // Save new Game to DB
    // Set current player as starting player
    // Create first welcome hand
    private fun create(ctx: Context): Nothing = TODO()

    private fun join(ctx: Context) = either {
        val request = validate<JoinGameRequest>(ctx.body())
        val player = player(ctx)
        val state = service.join(request, player)

        val seat = mapSeat(playerSeat(player, state.seats), state)
        val playedCards = currentTrick(state.tricks)!!.cardsByPosition()

        JoinGameResponse(state.game.uuid, seat, playedCards)
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

    private fun mapSeat(seat: Seat, state: GameState): SeatState {
        val player = playerAtPosition(seat.position, state.seats, state.allPlayers)!!
        val hand = currentHand(state.hands)!!
        val points = pointsByPositionTotal(completedHands(state.hands, state.tricks), state.tricks, state.seats)
        val cards = cardsInHand(hand, player, state)
        val nextState = nextState(state)
        val active = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)

        return SeatState(seat.uuid, cards, seat.position, player, points, nextState, active, hand.trump)
    }

    private fun play(ctx: Context) = either {
        val request = validate<PlayCardRequest>(ctx.body())
        val player = player(ctx)

        service.play(request, player)
        SuccessfulActionResponse()
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

    private fun trump(ctx: Context) = either {
        val request = validate<ChooseTrumpRequest>(ctx.body())
        val player = player(ctx)

        service.trump(request, player)
        SuccessfulActionResponse()
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

    private fun schiebe(ctx: Context) = either {
        val request = validate<SchiebeRequest>(ctx.body())
        val player = player(ctx)

        service.schiebe(request, player)
        SuccessfulActionResponse()
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )
}