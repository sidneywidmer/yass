package ch.yass

import arrow.core.raise.fold
import arrow.core.raise.recover
import ch.yass.core.error.CardNotPlayable
import ch.yass.core.error.PlayerDoesNotOwnCard
import ch.yass.admin.dsl.game
import ch.yass.game.GameService
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.engine.playerAtPosition
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance

class PlayCardTest : BaseTest() {
    private val service: GameService = Yass.container.direct.instance()

    /**
     * 4 Players in the game and the welcome hand is already played. Clubs is trump and in the first
     * trick a lot of trumps already went. Now in the second trick its WEST's turn.
     */
    private fun getState(): GameState {
        return game {
            players {
                north(name = "ueli", bot = false)
                east(name = "doris", bot = false)
                south(name = "christoph", bot = false)
                west(name = "daniela", bot = false)
            }
            hands {
                hand {
                    trump(Trump.FREESTYLE)
                    north(cards = "welcome", start = true)
                    east(cards = "welcome")
                    south(cards = "welcome")
                    west(cards = "welcome")
                    tricks {
                        trick(north = "WH", east = "WH", south = "WH", west = "WH")
                    }
                }
                hand {
                    trump(Trump.SPADES)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CQ,CA,D9,H8,H9,S7,SA")
                    west(cards = "C6,CK,D6,D8,DJ,DK,HJ,S6,S10")
                    tricks {
                        trick(north = "SJ", east = "S8", south = "S7", west = "S10")
                        trick(north = "S9", east = "SQ", south = "SA", west = null)
                    }
                }
            }
        }
    }

    @Test
    fun testPlayerCantPlayCardTwice() {
        val state = getState()

        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers)!!
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("CLUBS", "SEVEN", "french"))

        recover({ service.play(request, player) }) {
            assertTrue(it is PlayerDoesNotOwnCard)
        }
    }

    @Test
    fun testPlayerMustFollowSuitIfPossible() {
        val state = getState()

        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers)!!
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "JACK", "french"))

        fold(
            { service.play(request, player) },
            {
                val error = it as CardNotPlayable
                assertEquals(error.player, player)
            },
            { fail() }
        )
    }

    @Test
    fun testPlayerCanPlayValidCard() {
        val state = getState()

        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers)!!
        val playedCard = PlayedCard("SPADES", "SIX", "french")
        val request = PlayCardRequest(state.game.uuid.toString(), playedCard)

        fold(
            { service.play(request, player) },
            { fail() },
            {
                assertEquals(Card.from(playedCard), it.tricks[1].west)
                assertEquals(4, it.tricks.size)
            }
        )
    }
}