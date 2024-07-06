package ch.yass

import ch.yass.game.dto.*
import ch.yass.game.engine.blattWeise
import ch.yass.game.engine.gleicheWeise
import ch.yass.game.engine.possibleWeise
import org.junit.jupiter.api.Test

class WeisenTest : BaseTest() {

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
}