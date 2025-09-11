package ch.yass.integration

import arrow.core.raise.recover
import ch.yass.Yass
import ch.yass.admin.dsl.game
import ch.yass.game.GameService
import ch.yass.game.api.ChooseTrumpRequest
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.currentHand
import ch.yass.game.engine.playerAtPosition
import ch.yass.game.engine.tricksOfHand
import ch.yass.game.engine.winningPositionOfCurrentTrick
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance

class TrumpTest : Integration() {
    private val service: GameService = Yass.container.direct.instance()

    @Test
    fun testPlayerCanPlayUneufeTrump() {
        val state = game {
            hands {
                hand {
                    trump(Trump.NONE)
                    gschobe(Gschobe.NO)
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

        val player = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val request = ChooseTrumpRequest(state.game.uuid.toString(), "UNEUFE")

        val newState = recover({ service.trump(request, player) }) { fail() }
        assertEquals(Trump.UNEUFE, currentHand(newState.hands).trump)
    }

    @Test
    fun testCorrectPlayerWinsUneufeTrump() {
        val state = game {
            hands {
                hand {
                    trump(Trump.UNEUFE)
                    gschobe(Gschobe.NO)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CQ,CA,D9,H8,H9,S7,SA")
                    west(cards = "C6,CK,D6,D8,DJ,DK,HJ,S6,S10")
                    tricks {
                        trick(north = "C9", east = "C7", south = "CQ", west = "D6")
                    }
                }
            }
        }

        val currentHand = currentHand(state.hands)
        val currentTrick = tricksOfHand(state.tricks, currentHand)
        val wp = winningPositionOfCurrentTrick(currentHand, currentTrick)

        assertEquals(Position.EAST, wp)
    }
}