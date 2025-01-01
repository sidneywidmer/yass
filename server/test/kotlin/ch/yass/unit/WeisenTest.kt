package ch.yass.unit

import ch.yass.admin.dsl.interpretCard
import ch.yass.admin.dsl.interpretCards
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Trick
import ch.yass.game.engine.*
import ch.yass.game.engine.blattWeise
import ch.yass.game.engine.gleicheWeise
import ch.yass.game.engine.possibleWeise
import ch.yass.game.engine.possibleWeiseWithPoints
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

class WeisenTest {

    @Test
    fun testBlattWeise() {
        // 4 blatt clubs, 3 blatt hearts, yes I know this "hand" has too many cards :)
        val cards = listOf(
            Card(Suit.CLUBS, Rank.SIX, ""),
            Card(Suit.CLUBS, Rank.KING, ""),
            Card(Suit.CLUBS, Rank.QUEEN, ""),
            Card(Suit.CLUBS, Rank.SEVEN, ""),
            Card(Suit.CLUBS, Rank.EIGHT, ""),
            Card(Suit.CLUBS, Rank.JACK, ""),
            Card(Suit.CLUBS, Rank.ACE, ""),
            Card(Suit.HEARTS, Rank.NINE, ""),
            Card(Suit.HEARTS, Rank.TEN, ""),
            Card(Suit.HEARTS, Rank.JACK, ""),
            Card(Suit.DIAMONDS, Rank.NINE, ""),
        )

        val result = blattWeise(cards)
        val vierBlatt = result.firstOrNull { it.type == WeisType.VIER_BLATT }
        val dreiBlatt = result.firstOrNull { it.type == WeisType.DREI_BLATT }

        assert(vierBlatt !== null)
        assert(dreiBlatt !== null)
        assert(vierBlatt?.cards?.size == 4)
        assert(dreiBlatt?.cards?.size == 3)
    }

    @Test
    fun testWeisPoints() {
        val cards = listOf(
            Card(Suit.CLUBS, Rank.SIX, ""),
            Card(Suit.CLUBS, Rank.KING, ""),
            Card(Suit.CLUBS, Rank.QUEEN, ""),
            Card(Suit.CLUBS, Rank.SEVEN, ""),
            Card(Suit.CLUBS, Rank.EIGHT, ""),
            Card(Suit.CLUBS, Rank.JACK, ""),
            Card(Suit.CLUBS, Rank.ACE, ""),
            Card(Suit.HEARTS, Rank.NINE, ""),
            Card(Suit.HEARTS, Rank.TEN, ""),
            Card(Suit.HEARTS, Rank.JACK, ""),
            Card(Suit.DIAMONDS, Rank.NINE, ""),
        )

        val result = possibleWeiseWithPoints(cards, Trump.CLUBS)
        assert(result.sumOf { it.points } == 90)
    }

    @Test
    fun testGleicheWeise() {
        val cards = listOf(
            Card(Suit.CLUBS, Rank.SIX, ""),
            Card(Suit.HEARTS, Rank.SIX, ""),
            Card(Suit.SPADES, Rank.SIX, ""),
            Card(Suit.DIAMONDS, Rank.SIX, ""),
        )

        val result = gleicheWeise(cards)
        assert(result.firstOrNull { it.type == WeisType.VIER_GLEICHE } !== null)
    }

    @Test
    fun testGleicheWeiseBuur() {
        val cards = listOf(
            Card(Suit.CLUBS, Rank.JACK, ""),
            Card(Suit.HEARTS, Rank.JACK, ""),
            Card(Suit.SPADES, Rank.JACK, ""),
            Card(Suit.DIAMONDS, Rank.JACK, ""),
        )

        val result = gleicheWeise(cards)
        assert(result.firstOrNull { it.type == WeisType.VIER_BUUR } !== null)
    }

    @Test
    fun testGleicheWeiseNell() {
        val cards = listOf(
            Card(Suit.CLUBS, Rank.NINE, ""),
            Card(Suit.HEARTS, Rank.NINE, ""),
            Card(Suit.SPADES, Rank.NINE, ""),
            Card(Suit.DIAMONDS, Rank.NINE, ""),
        )

        val result = gleicheWeise(cards)
        assert(result.firstOrNull { it.type == WeisType.VIER_NELL } !== null)
    }

    @Test
    fun testSkipWeisOnlyIfOtherPresent() {
        val cards = listOf(Card(Suit.CLUBS, Rank.EIGHT, ""))
        val result = possibleWeise(cards, Trump.CLUBS)
        assert(result.firstOrNull { it.type == WeisType.SKIP } === null)
    }

    @Test
    fun testSkipWeis() {
        val cards = listOf(
            Card(Suit.CLUBS, Rank.NINE, ""),
            Card(Suit.HEARTS, Rank.NINE, ""),
            Card(Suit.SPADES, Rank.NINE, ""),
            Card(Suit.DIAMONDS, Rank.NINE, ""),
        )
        val result = possibleWeise(cards, Trump.CLUBS)
        assert(result.firstOrNull { it.type == WeisType.SKIP } !== null)
    }

    @Test
    fun testCrossWeis() {
        val cards = listOf(
            Card(Suit.CLUBS, Rank.EIGHT, ""),
            Card(Suit.CLUBS, Rank.NINE, ""),
            Card(Suit.CLUBS, Rank.TEN, ""),
            Card(Suit.HEARTS, Rank.NINE, ""),
            Card(Suit.SPADES, Rank.NINE, ""),
            Card(Suit.DIAMONDS, Rank.NINE, ""),
            Card(Suit.CLUBS, Rank.QUEEN, ""),
            Card(Suit.CLUBS, Rank.KING, ""),
        )

        val result = possibleWeise(cards, Trump.CLUBS)
        assert(result.firstOrNull { it.type == WeisType.VIER_NELL } !== null)
        assert(result.firstOrNull { it.type == WeisType.DREI_BLATT } !== null)
        assert(result.firstOrNull { it.type == WeisType.STOECK } !== null)
    }

    @Test
    fun testisAlreadyGewiesenSecond() {
        val trick = Trick(
            1,
            UUID.randomUUID(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            1,
            interpretCard("S6"),
            interpretCard("D6"),
            interpretCard("H6"),
            interpretCard("C6")
        )

        val hand = Hand(
            1,
            UUID.randomUUID(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            Position.NORTH,
            1,
            Trump.CLUBS,
            Gschobe.NO,
            interpretCards("S6,S7,S8,S9,S10,SJ,DJ,CJ,HJ"), // 4 jacks, 6 blatt
            interpretCards("D6,D7,D8,D9,D10,SQ,DQ,DK,DA"),
            interpretCards("C6,C7,C8,C9,C10,SK,CQ,CK,CA"),
            interpretCards("H6,H7,H8,H9,H10,SA,HQ,HK,HA"),
            listOf(Weis(WeisType.VIER_BUUR, interpretCards("SJ,DJ,CJ,HJ"))),
            emptyList(),
            listOf(Weis(WeisType.FUENF_BLATT, interpretCards("C6,C7,C8,C9,C10"))),
            emptyList(),
        )

        // N/S won the weis in the first round, so now they play another one since north still has SECHS_BLATT
        // meaning they can still play this weis
        val result = isAlreadyGewiesenSecond(listOf(trick), hand)
        assert(!result)
    }
}
