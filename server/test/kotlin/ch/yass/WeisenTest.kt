package ch.yass

import ch.yass.game.dto.Card
import ch.yass.game.dto.Rank
import ch.yass.game.dto.Suit
import ch.yass.game.engine.maxBlatt
import org.junit.jupiter.api.Test

class WeisenTest : BaseTest() {

    @Test
    fun testGetLongestSequence() {
        val cards = listOf(
            Card(Suit.CLUBS, Rank.SIX, ""),
            Card(Suit.CLUBS, Rank.KING, ""),
            Card(Suit.CLUBS, Rank.SEVEN, ""),
            Card(Suit.CLUBS, Rank.EIGHT, ""),
            Card(Suit.CLUBS, Rank.ACE, ""),
        )

        val foo = maxBlatt(cards)
        val bar = "foo"
    }
}