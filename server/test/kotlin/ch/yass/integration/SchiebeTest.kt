package ch.yass.integration

import arrow.core.raise.recover
import ch.yass.Yass
import ch.yass.admin.dsl.game
import ch.yass.core.error.InvalidState
import ch.yass.core.error.PlayerIsLocked
import ch.yass.game.GameService
import ch.yass.game.api.ChooseTrumpRequest
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.api.SchiebeRequest
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.State
import ch.yass.game.dto.Trump
import ch.yass.game.engine.nextState
import ch.yass.game.engine.playerAtPosition
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance

class SchiebeTest : Integration() {
    private val service: GameService = Yass.container.direct.instance()

    private fun getState(): GameState {
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
    fun testPlayerCantPlayCardIfNotYetGschobe() {
        val state = getState()

        val player = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("CLUBS", "NINE", "french"))

        recover({ service.play(request, player) }) {
            assertTrue(it is InvalidState)
        }
    }

    @Test
    fun testAfterSchiebeCorrectPlayersTurn() {
        val state = getState()

        val playerNorth = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val playerSouth = playerAtPosition(Position.SOUTH, state.seats, state.allPlayers)
        val schiebeRequest = SchiebeRequest(state.game.uuid.toString(), Gschobe.YES.name)
        val trumpRequest = ChooseTrumpRequest(state.game.uuid.toString(), "CLUBS")

        val newState1 = recover({ service.schiebe(schiebeRequest, playerNorth) }) { fail() }
        assertEquals(State.TRUMP, nextState(newState1))

        val newState2 = recover({ service.trump(trumpRequest, playerNorth) }) {
            assertTrue(it is PlayerIsLocked)
            newState1
        }
        assertEquals(State.TRUMP, nextState(newState2))

        val newState3 = recover({ service.trump(trumpRequest, playerSouth) }) { fail() }
        assertEquals(State.PLAY_CARD, nextState(newState3))
    }
}