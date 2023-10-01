package ch.yass.admin

import arrow.core.raise.either
import arrow.core.raise.recover
import ch.yass.admin.api.GenerateHandResponse
import ch.yass.admin.dsl.game
import ch.yass.core.contract.Controller
import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.successResponse
import ch.yass.game.GameService
import ch.yass.game.api.GameStateResponse
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.cardToNotation
import ch.yass.game.engine.playerAtPosition
import ch.yass.game.engine.randomHand
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.Context


class AdminController(private val gameService: GameService) : Controller {
    override val path = "/admin"

    override val endpoints = EndpointGroup {
        get("/generate/hand", ::generateHand)
        get("/generate/game", ::generateGame)
        get("/analyze/game/{code}", ::analyzeGame)
    }

    private fun analyzeGame(ctx: Context) = either {
        gameService.getStateByCode(ctx.pathParam("code"))
    }.fold(
        { errorResponse(ctx, it) },
        { successResponse(ctx, it) }
    )

    private fun generateGame(ctx: Context) {
        val state = game {
            players {
                north(name = "ueli", bot = true)
                east(name = "doris", bot = true)
                south(name = "christoph", bot = true)
                west(name = "daniela", bot = true)
            }
            hands {
                hand {
                    trump(Trump.FREESTYLE)
                    north(cards = "welcome", start = true)
                    east(cards = "welcome")
                    south(cards = "welcome")
                    west(cards = "welcome")
                    tricks {
                        trick(north = "WH", east = "WH", south = "WH", west = "WH")
                    }
                }
                hand {
                    trump(Trump.SPADES)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CQ,CA,D9,H8,H9,S7,SA")
                    west(cards = "C6,CK,D6,D8,DJ,DK,HJ,S6,S10")
                    tricks { }
                }
            }
        }

        val player = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)!!
        val playedCard = PlayedCard("CLUBS", "NINE", "french")
        val request = PlayCardRequest(state.game.uuid.toString(), playedCard)
        val newState = recover({ gameService.play(request, player) }, { throw Exception("something went wrong") })

        successResponse(ctx, object {
            val gameCode = newState.game.code
        })
    }

    private fun generateHand(ctx: Context) {
        val hand = randomHand()
        successResponse(
            ctx, GenerateHandResponse(
                north = hand[Position.NORTH]!!.joinToString(",") { cardToNotation(it) },
                east = hand[Position.EAST]!!.joinToString(",") { cardToNotation(it) },
                south = hand[Position.SOUTH]!!.joinToString(",") { cardToNotation(it) },
                west = hand[Position.WEST]!!.joinToString(",") { cardToNotation(it) },
            )
        )
    }
}