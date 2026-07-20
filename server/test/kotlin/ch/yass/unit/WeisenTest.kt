package ch.yass.unit

import ch.yass.admin.dsl.TricksBuilder
import ch.yass.admin.dsl.inMemoryGame
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
        val cards = interpretCards("C6,CK,CQ,C7,C8,CJ,CA,H9,H10,HJ,D9")

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
        val cards = interpretCards("C6,CK,CQ,C7,C8,CJ,CA,H9,H10,HJ,D9")

        val result = possibleWeiseWithPoints(cards, Trump.CLUBS)
        assert(result.sumOf { it.points } == 180)
    }

    @Test
    fun testGleicheWeise() {
        val cards = interpretCards("C6,H6,S6,D6")

        val result = gleicheWeise(cards)
        assert(result.firstOrNull { it.type == WeisType.VIER_GLEICHE } !== null)
    }

    @Test
    fun testGleicheWeiseBuur() {
        val cards = interpretCards("CJ,HJ,SJ,DJ")

        val result = gleicheWeise(cards)
        assert(result.firstOrNull { it.type == WeisType.VIER_BUUR } !== null)
    }

    @Test
    fun testGleicheWeiseNell() {
        val cards = interpretCards("C9,H9,S9,D9")

        val result = gleicheWeise(cards)
        assert(result.firstOrNull { it.type == WeisType.VIER_NELL } !== null)
    }

    @Test
    fun testSkipWeisOnlyIfOtherPresent() {
        val cards = interpretCards("C8")
        val result = possibleWeise(cards, Trump.CLUBS)
        assert(result.firstOrNull { it.type == WeisType.SKIP } === null)
    }

    @Test
    fun testSkipWeis() {
        val cards = interpretCards("C9,H9,S9,D9")
        val result = possibleWeise(cards, Trump.CLUBS)
        assert(result.firstOrNull { it.type == WeisType.SKIP } !== null)
    }

    @Test
    fun testCrossWeis() {
        val cards = interpretCards("C8,C9,C10,H9,S9,D9,CQ,CK")

        val result = possibleWeise(cards, Trump.CLUBS)
        assert(result.firstOrNull { it.type == WeisType.VIER_NELL } !== null)
        assert(result.firstOrNull { it.type == WeisType.DREI_BLATT } !== null)
        assert(result.firstOrNull { it.type == WeisType.STOECK } !== null)
    }

    /**
     * Regression test: in the previous hand north played a king and a queen (of any suit). In the new
     * hand north holds the stoeck (CK, CQ) and just played its first card - a card that is not part
     * of the stoeck. Cards of previous hands must not trigger the automatic stoeck weis.
     */
    @Test
    fun testStoeckNotWeisedAtStartOfNewHand() {
        val state = inMemoryGame {
            hands {
                hand {
                    trump(Trump.SPADES)
                    gschobe(Gschobe.NO)
                    north(cards = "SK,DQ,C6,C7,C8,C9,C10,CJ,CA", start = true)
                    east(cards = "D6,D7,D8,D9,D10,DJ,DK,DA,H6")
                    south(cards = "H7,H8,H9,H10,HJ,HQ,HK,HA,S6")
                    west(cards = "S7,S8,S9,S10,SJ,SQ,SA,CQ,CK")
                    tricks {
                        trick(north = "SK", east = "D6", south = "H7", west = "S7")
                        trick(north = "DQ", east = "D7", south = "H8", west = "S8")
                        trick(north = "C6", east = "D8", south = "H9", west = "S9")
                        trick(north = "C7", east = "D9", south = "H10", west = "S10")
                        trick(north = "C8", east = "D10", south = "HJ", west = "SJ")
                        trick(north = "C9", east = "DJ", south = "HQ", west = "SQ")
                        trick(north = "C10", east = "DK", south = "HK", west = "SA")
                        trick(north = "CJ", east = "DA", south = "HA", west = "CQ")
                        trick(north = "CA", east = "H6", south = "S6", west = "CK")
                    }
                }
                hand {
                    trump(Trump.CLUBS)
                    gschobe(Gschobe.NO)
                    north(cards = "CK,CQ,S6,S8,H6,H8,D6,D8,D10", start = true)
                    east(cards = "C6,C7,C8,S7,S9,H7,H9,D7,D9")
                    south(cards = "C9,C10,CJ,S10,SJ,H10,HJ,DJ,DQ")
                    west(cards = "CA,SQ,SK,SA,HQ,HK,HA,DK,DA")
                    tricks {
                        trick(north = "S6", east = null, south = null, west = null)
                    }
                }
            }
        }

        // Same check as in GameService.play right after north played S6
        val hand = currentHand(state.hands)
        val weise = possibleWeise(hand.cardsOf(Position.NORTH), hand.trump)
        assert(!shouldWeisStoeck(hand, weise, Position.NORTH, tricksOfHand(state.tricks, hand)))
    }

    private fun stoeckGameWithTricks(lambda: TricksBuilder.() -> Unit) = inMemoryGame {
        hands {
            hand {
                trump(Trump.CLUBS)
                gschobe(Gschobe.NO)
                north(cards = "CK,CQ,SK,HQ,C7,S6,H6,D6,D8", start = true)
                east(cards = "S7,S8,S9,H7,H8,H9,D7,D9,C6")
                south(cards = "S10,SJ,SQ,H10,HJ,HK,D10,DJ,C8")
                west(cards = "SA,HA,DQ,DK,DA,C9,C10,CJ,CA")
                tricks(lambda)
            }
        }
    }

    @Test
    fun testStoeckNotWeisedForSameRanksOfOtherSuits() {
        // North played a king and a queen, but not the trump ones - the stoeck is still in hand
        val state = stoeckGameWithTricks {
            trick(north = "SK", east = "S7", south = "S10", west = "SA")
            trick(north = "HQ", east = "H7", south = "H10", west = "HA")
        }

        val hand = currentHand(state.hands)
        val weise = possibleWeise(hand.cardsOf(Position.NORTH), hand.trump)
        assert(!shouldWeisStoeck(hand, weise, Position.NORTH, tricksOfHand(state.tricks, hand)))
    }

    @Test
    fun testStoeckWeisedOnceSecondCardOfPairIsPlayed() {
        val state = stoeckGameWithTricks {
            trick(north = "CK", east = "C6", south = "C8", west = "C9")
            trick(north = "CQ", east = "S7", south = "S10", west = "CA")
        }

        val hand = currentHand(state.hands)
        val weise = possibleWeise(hand.cardsOf(Position.NORTH), hand.trump)
        assert(shouldWeisStoeck(hand, weise, Position.NORTH, tricksOfHand(state.tricks, hand)))
    }

    @Test
    fun testisAlreadyGewiesenSecond() {
        // TODO: Update with inMemoryGame once it supports weise
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
