package ch.yass.unit

import ch.yass.admin.dsl.interpretCards
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.generateHand
import ch.yass.game.engine.sortByPoints
import ch.yass.game.engine.upcomingPositions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EngineHelpersTest {

    @Test
    fun testSortByValueSuit() {
        val result = sortByPoints(interpretCards("C6,CJ,C10,CK"), Trump.CLUBS)
        assertTrue(result == interpretCards("CJ,CK,C10,C6"))
    }

    @Test
    fun testUpcomingPositions() {
        // Test case 1: Current is first player (lead)
        val result1 = upcomingPositions(Position.NORTH, Position.NORTH)
        assertTrue(result1.size == 3)
        assertTrue(!result1.contains(Position.NORTH))

        // Test case 2: Current is second player
        val result2 = upcomingPositions(Position.NORTH, Position.WEST)
        assertTrue(result2.size == 2)
        assertTrue(!result2.contains(Position.WEST))
        assertTrue(!result2.contains(Position.NORTH))

        // Test case 3: Current is third player
        val result3 = upcomingPositions(Position.NORTH, Position.SOUTH)
        assertTrue(result3.size == 1)

        // Test case 4: Current is last player
        val result4 = upcomingPositions(Position.NORTH, Position.EAST)
        assertTrue(result4.isEmpty())

        // Test case 5: Different lead position
        val result5 = upcomingPositions(Position.WEST, Position.SOUTH)
        assertTrue(result5.size == 2)
        assertTrue(result5.contains(Position.EAST))
        assertTrue(result5.contains(Position.NORTH))
        assertTrue(!result5.contains(Position.WEST))
        assertTrue(!result5.contains(Position.SOUTH))

        // Test case 6: Verify order is maintained
        val result6 = upcomingPositions(Position.SOUTH, Position.SOUTH)
        assertTrue(result6.size == 3)
        assertTrue(result6[0] == Position.EAST)
        assertTrue(result6[1] == Position.NORTH)
        assertTrue(result6[2] == Position.WEST)
    }

    /**
     * Wo don't trust kotlin or the jvm, just make sure a fixed seed always generates the same hand.
     */
    @Test
    fun testGenerateHandIsStableForAFixedSeed() {
        val hand = generateHand(seed = 123456, handNumber = 0)

        assertTrue(hand.getValue(Position.NORTH) == interpretCards("C10,CK,D6,DJ,DA,HJ,HA,S8,SJ"))
        assertTrue(hand.getValue(Position.WEST) == interpretCards("C7,CQ,H6,H7,H9,H10,HQ,S10,SK"))
        assertTrue(hand.getValue(Position.SOUTH) == interpretCards("C6,C8,D7,D9,DK,H8,HK,S9,SA"))
        assertTrue(hand.getValue(Position.EAST) == interpretCards("C9,CJ,CA,D8,D10,DQ,S6,S7,SQ"))
    }
}
