package ch.yass.unit

import ch.yass.admin.dsl.interpretCards
import ch.yass.game.bot.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChooseTrumpTest {
    @Test
    fun testJackWithThreeOrMoreTrumps() {
        val candidate = getTrumpCandidate(interpretCards("SJ,S8,S7,S6,H10,HK,CQ,DA,CA"))
        assertTrue { candidate.reason is JackWithThreeOrMoreTrumps }
    }

    @Test
    fun testPerfectTrio() {
        val candidate = getTrumpCandidate(interpretCards("HJ,H9,HA,S10,SK,CQ,D7,D8,CA"))
        assertTrue { candidate.reason is PerfectTrio }
    }

    @Test
    fun testJackNineWithTwoAdditional() {
        val candidate = getTrumpCandidate(interpretCards("CJ,C9,C8,C7,S10,HK,DQ,DA,DK"))
        assertTrue { candidate.reason is JackNineWithTwoAdditional }
    }

    @Test
    fun testNineWithFiveOrMoreTrumps() {
        val candidate = getTrumpCandidate(interpretCards("D9,D8,D7,D6,D10,SK,HJ,CQ,CA"))
        assertTrue { candidate.reason is NineWithFiveOrMoreTrumps }
    }

    @Test
    fun testSixPlusTrumpsWithPowerCard() {
        val candidate = getTrumpCandidate(interpretCards("SA,S10,S8,S7,S6,SQ,HK,CQ,CA"))
        assertTrue { candidate.reason is SixPlusTrumpsWithPowerCard }
    }

    @Test
    fun testJackWithTwoTrumps() {
        val candidate = getTrumpCandidate(interpretCards("SJ,S8,S7,H10,HK,CQ,D9,DA,CA"))
        assertTrue { candidate.reason is JackWithTwoTrumps }
    }

    @Test
    fun testNineWithFourTrumps() {
        val candidate = getTrumpCandidate(interpretCards("D9,D8,D7,D6,S10,HK,CQ,CA,HQ"))
        assertTrue { candidate.reason is NineWithFourTrumps }
    }

    @Test
    fun testFiveTrumpsWithAce() {
        val candidate = getTrumpCandidate(interpretCards("HA,H8,H7,H6,H10,SK,CQ,D7,DA"))
        assertTrue { candidate.reason is FiveTrumpsWithAce }
    }

    @Test
    fun testNineJackStrongSides() {
        val candidate = getTrumpCandidate(interpretCards("HJ,H9,H8,SA,SK,CQ,DK,D7"))
        assertTrue { candidate.reason is NineJackStrongSides }
    }

    @Test
    fun testFallback() {
        val candidate = getTrumpCandidate(interpretCards("S8,S7,H8,H7,D8,D7,C8,C7,C6"))
        assertTrue { candidate.reason is TrumpFallback }
    }

    @Test
    fun testStrongUneufe() {
        val candidate = getTrumpCandidate(interpretCards("S6,H6,D7,C6,S7,H7,S8,D8,C9"))
        assertTrue { candidate.reason is StrongUneufe }
    }

    @Test
    fun testStrongObenabe() {
        val candidate = getTrumpCandidate(interpretCards("SA,HA,DA,SK,HK,CQ,SQ,HQ,C10"))
        assertTrue { candidate.reason is StrongObenabe }
    }

}
