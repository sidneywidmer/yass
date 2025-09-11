package ch.yass.integration

import ch.yass.admin.dsl.game
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.dto.sumPointsByPosition
import ch.yass.game.engine.handTricksWithPoints
import ch.yass.game.engine.tricksWithPoints
import ch.yass.game.engine.tricksOfHand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CountingPointsTest : Integration() {
    private fun getStateSpades(): GameState {
        return game {
            hands {
                hand {
                    trump(Trump.SPADES)
                    gschobe(Gschobe.NO)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CQ,CA,D9,H8,H9,S7,SA")
                    west(cards = "C6,CK,D6,D8,DJ,DK,HJ,S6,S10")
                    tricks {
                        trick(north = "SJ", east = "SK", south = "SA", west = "S10")  // 45 for North
                    }
                }
            }
        }
    }

    @Test
    fun testTrump() {
        val state = getStateSpades()

        val points = handTricksWithPoints(state.hands, state.tricks).sumPointsByPosition()
        assertEquals(45, points[Position.NORTH])
    }

}