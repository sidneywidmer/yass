package ch.yass.unit

import ch.yass.admin.dsl.interpretCards
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
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
}
