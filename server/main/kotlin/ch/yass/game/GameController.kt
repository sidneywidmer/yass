package ch.yass.game

import arrow.core.raise.either
import arrow.core.raise.ensure
import ch.yass.Yass
import ch.yass.admin.dsl.interpretCards
import ch.yass.core.contract.Controller
import ch.yass.core.error.PlayerDoesNotOwnSeat
import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.successResponse
import ch.yass.core.helper.toUUID
import ch.yass.core.helper.validate
import ch.yass.game.api.*
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.CardOnTable
import ch.yass.game.dto.PlayerAtTable
import ch.yass.game.dto.Position
import ch.yass.game.dto.SeatState
import ch.yass.game.dto.SeatStatus
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.*
import ch.yass.identity.helper.player
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context
import org.kodein.di.direct
import org.kodein.di.instance
import java.time.LocalDateTime
import java.time.ZoneOffset

class GameController(private val service: GameService, private val repo: GameRepository) : Controller {
    override val path = "/game"

    override val endpoints = EndpointGroup {
        post("/create", ::create)
        post("/join", ::join)
        post("/play", ::play)
        post("/trump", ::trump)
        post("/weisen", ::weisen)
        post("/schiebe", ::schiebe)
        post("/ping", ::ping)
    }

    private fun ping(ctx: Context) = either {
        val request = validate<PingSeatRequest>(ctx.body())
        val player = player(ctx)
        val state = repo.getState(repo.getBySeatUUID(request.seat))
        val seat = state.seats.first { it.uuid == request.seat.toUUID() }

        ensure(playerOwnsSeat(player, request.seat, state.seats)) { PlayerDoesNotOwnSeat(player, request.seat, state) }

        if (seat.status == SeatStatus.DISCONNECTED) {
            service.connectSeat(seat)
        }

        repo.pingSeat(seat, LocalDateTime.now(ZoneOffset.UTC))

        SuccessfulActionResponse()
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

    private fun create(ctx: Context) = either {
        val request = validate<CreateCustomGameRequest>(ctx.body())
        val player = player(ctx)
        val code = service.create(request, player)

        CreateCustomGameResponse(code)
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

    private fun join(ctx: Context) = either {
        val request = validate<JoinGameRequest>(ctx.body())
        val player = player(ctx)
        val state = service.join(request, player)

        val seat = mapSeat(playerSeat(player, state.seats), state)
        val playedCards = currentTrick(state.tricks).cardsByPosition()
        val otherPlayers = Position.entries
            .map { pos -> maybePlayerAtPosition(pos, state.seats, state.allPlayers) }
            .filterNotNull()
            .map { p -> Pair(p, playerSeat(p, state.seats)) }
            .map { pair ->
                PlayerAtTable(
                    pair.first.uuid,
                    pair.first.name,
                    pair.first.bot,
                    pair.second.position,
                    pair.second.status
                )
            }

        JoinGameResponse(state.game.uuid, state.game.code, seat, playedCards, otherPlayers)
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

    private fun mapSeat(seat: Seat, state: GameState): SeatState {
        val player = playerAtPosition(seat.position, state.seats, state.allPlayers)
        val hand = currentHand(state.hands)
        val points = pointsByPositionTotal(state.hands, state.tricks)
        val cards = cardsInHand(hand, player, state)
        val nextState = nextState(state)
        val active = activePosition(state.hands, state.seats, state.tricks)
        val weise = possibleWeiseWithPoints(hand.cardsOf(seat.position), hand.trump)

        return SeatState(seat.uuid, cards, seat.position, player, points, nextState, active, hand.trump, weise)
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

    private fun weisen(ctx: Context) = either {
        val request = validate<WeisenRequest>(ctx.body())
        val player = player(ctx)

        service.weisen(request, player)
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