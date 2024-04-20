package ch.yass

import ch.yass.admin.dsl.game
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.pointsByPosition
import ch.yass.game.engine.tricksOfHand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CountingPointsTest : BaseTest() {
    private fun getStateSpades(): GameState {
        return game {
            players {
                north(name = "ueli", bot = true)
                east(name = "doris", bot = true)
                south(name = "christoph", bot = true)
                west(name = "daniela", bot = true)
            }
            hands {
                hand {
                    trump(Trump.SPADES)
                    gschobe(Gschobe.NO)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CQ,CA,D9,H8,H9,S7,SA")
                    west(cards = "C6,CK,D6,D8,DJ,DK,HJ,S6,S10")
                    tricks {
                        trick(north = "SJ", east = "SK", south = "SA", west = "S10")  // 45 for N + 5 for last
                    }
                }
            }
        }
    }

    @Test
    fun testTrump() {
        val state = getStateSpades()
        val hand = state.hands.first()

        val points = pointsByPosition(hand, tricksOfHand(state.tricks, hand), state.seats)
        assertEquals(50, points[Position.NORTH])
    }

}