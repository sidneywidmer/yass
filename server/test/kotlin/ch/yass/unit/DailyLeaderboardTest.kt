package ch.yass.unit

import ch.yass.admin.dsl.inMemoryGame
import ch.yass.game.api.internal.DailyGame
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.dailyLeaderboardEntries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DailyLeaderboardTest {

    /**
     * A daily run played to the end, always with the player sitting south like in the real challenge. The hand
     * has two tricks, the one with the trump buur is worth 90 points and the one with the nell 34, so whoever
     * wins them decides if the team of the player (NS) beat the bots and with how many points.
     */
    private fun dailyGame(player: String, buurTrickWinner: Position, nellTrickWinner: Position): DailyGame {
        val buurTrick = trickCards(buurTrickWinner, "SJ", listOf("SK", "SA", "S10"))
        val nellTrick = trickCards(nellTrickWinner, "S9", listOf("SQ", "S7", "S6"))
        val dealt = Position.entries.associateWith { "${buurTrick.getValue(it)},${nellTrick.getValue(it)}" }

        val state = inMemoryGame {
            hands {
                hand {
                    trump(Trump.SPADES)
                    gschobe(Gschobe.NO)
                    north(cards = dealt.getValue(Position.NORTH), start = true)
                    east(cards = dealt.getValue(Position.EAST))
                    south(cards = dealt.getValue(Position.SOUTH))
                    west(cards = dealt.getValue(Position.WEST))
                    tricks {
                        trick(
                            north = buurTrick.getValue(Position.NORTH),
                            east = buurTrick.getValue(Position.EAST),
                            south = buurTrick.getValue(Position.SOUTH),
                            west = buurTrick.getValue(Position.WEST)
                        )
                        trick(
                            north = nellTrick.getValue(Position.NORTH),
                            east = nellTrick.getValue(Position.EAST),
                            south = nellTrick.getValue(Position.SOUTH),
                            west = nellTrick.getValue(Position.WEST)
                        )
                    }
                }
            }
        }

        return DailyGame(state.game, player, Position.SOUTH, state.hands, state.tricks)
    }

    /**
     * Everyone follows suit, so [winningCard] (the highest trump of the trick) decides who takes it.
     */
    private fun trickCards(winner: Position, winningCard: String, rest: List<String>): Map<Position, String> =
        mapOf(winner to winningCard) + Position.entries.filter { it != winner }.zip(rest)

    @Test
    fun testLostRunStaysOnTheBoardWithZeroPoints() {
        val lost = dailyGame("loser", buurTrickWinner = Position.EAST, nellTrickWinner = Position.SOUTH)

        val entries = dailyLeaderboardEntries(listOf(lost))

        assertEquals(1, entries.size)
        assertEquals("loser", entries.first().player)
        assertEquals(0, entries.first().points) // 34 points collected on the way, but the bots won
    }

    @Test
    fun testWonRunScoresThePointsOfTheWholeTeam() {
        val won = dailyGame("winner", buurTrickWinner = Position.SOUTH, nellTrickWinner = Position.NORTH)

        val entries = dailyLeaderboardEntries(listOf(won))

        assertEquals(124, entries.first().points) // 90 of the player plus 34 of their partner
    }

    @Test
    fun testEntriesAreRankedAndLimited() {
        val games = listOf(
            dailyGame("lost", buurTrickWinner = Position.EAST, nellTrickWinner = Position.SOUTH),
            dailyGame("close", buurTrickWinner = Position.SOUTH, nellTrickWinner = Position.EAST),
            dailyGame("best", buurTrickWinner = Position.SOUTH, nellTrickWinner = Position.NORTH)
        )

        assertEquals(listOf("best", "close", "lost"), dailyLeaderboardEntries(games).map { it.player })
        assertEquals(listOf(124, 90, 0), dailyLeaderboardEntries(games).map { it.points })
        assertEquals(listOf("best", "close"), dailyLeaderboardEntries(games, limit = 2).map { it.player })
    }
}
