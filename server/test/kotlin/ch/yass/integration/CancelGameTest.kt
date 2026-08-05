package ch.yass.integration

import arrow.core.raise.recover
import ch.yass.admin.dsl.game
import ch.yass.core.error.GameAlreadyCanceled
import ch.yass.core.error.PlayerDidNotCreateGame
import ch.yass.game.GameRepository
import ch.yass.game.GameService
import ch.yass.game.PlayerService
import ch.yass.game.api.CancelGameRequest
import ch.yass.game.api.JoinGameRequest
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.api.internal.GameState
import ch.yass.game.api.internal.NewAnonPlayer
import ch.yass.game.dto.GameStatus
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.playerAtPosition
import ch.yass.game.engine.positionSeat
import ch.yass.game.pubsub.GameCanceled
import ch.yass.integration.helper.CentrifugoTestHelper
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance

class CancelGameTest : Integration() {
    private val service: GameService = container.direct.instance()
    private val playerService: PlayerService = container.direct.instance()
    private val repo: GameRepository = container.direct.instance()
    private val cth = CentrifugoTestHelper(container.direct.instance())

    /**
     * Default players are seated NORTH, EAST, SOUTH, WEST in that order, so NORTH holds the lowest seat
     * id and counts as the creator (see playerCreatedGame).
     */
    private fun getState(): GameState {
        centrifugo.resetRequests()
        return game {
            hands {
                hand {
                    trump(Trump.NONE)
                    gschobe(Gschobe.NOT_YET)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CQ,CA,D9,H8,H9,S7,SA")
                    west(cards = "C6,CK,D6,D8,DJ,DK,HJ,S6,S10")
                    tricks {
                        trick(north = null, east = null, south = null, west = null)
                    }
                }
            }
        }
    }

    @Test
    fun testGameCanceledIsPublishedToEveryoneButTheCanceler() {
        val state = getState()
        val creator = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)

        val canceled = recover({
            service.cancel(CancelGameRequest(state.game.uuid.toString()), creator)
        }) { fail() }

        assertThat(canceled.status, equalTo(GameStatus.CANCELED))
        assertThat(repo.getState(state.game).game.status, equalTo(GameStatus.CANCELED))

        val actions = cth.parseActions(centrifugo.allServeEvents)

        listOf(Position.EAST, Position.SOUTH, Position.WEST).forEach { position ->
            cth.assertActions(positionSeat(position, state.seats).uuid, actions).apply {
                hasCount(GameCanceled::class, 1)
                hasCanceledBy(creator)
            }
        }

        cth.assertActions(positionSeat(Position.NORTH, state.seats).uuid, actions).apply {
            hasCount(GameCanceled::class, 0)
        }
    }

    @Test
    fun testJoiningACanceledGameFails() {
        val state = getState()
        val creator = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)

        recover({ service.cancel(CancelGameRequest(state.game.uuid.toString()), creator) }) { fail() }

        val newcomer = playerService.create(NewAnonPlayer("Latecomer", "HASHED_TOKEN"))
        recover({ service.join(JoinGameRequest(state.game.code), newcomer) }) {
            assertTrue(it is GameAlreadyCanceled)
        }

        // Nobody was seated by the failed join, the table is exactly as it was left
        assertThat(repo.getState(state.game).seats.size, equalTo(4))
    }

    @Test
    fun testCanceledGameTakesNoMoreActions() {
        val state = getState()
        val creator = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)

        recover({ service.cancel(CancelGameRequest(state.game.uuid.toString()), creator) }) { fail() }

        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("CLUBS", "NINE"))
        recover({ service.play(request, creator) }) {
            assertTrue(it is GameAlreadyCanceled)
        }
    }

    @Test
    fun testOnlyTheCreatorCanCancel() {
        val state = getState()
        val other = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)

        recover({ service.cancel(CancelGameRequest(state.game.uuid.toString()), other) }) {
            assertTrue(it is PlayerDidNotCreateGame)
        }

        assertThat(repo.getState(state.game).game.status, equalTo(GameStatus.RUNNING))
    }
}
