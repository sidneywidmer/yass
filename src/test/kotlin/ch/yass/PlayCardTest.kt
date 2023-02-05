package ch.yass

import org.junit.Test
import ch.yass.dsl.game
import ch.yass.game.GameService
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.engine.playerAtPosition
import junit.framework.TestCase.assertEquals
import org.kodein.di.direct
import org.kodein.di.instance

class PlayCardTest : BaseTest() {
    private val service: GameService = container.direct.instance()

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

        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers).unnest()
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("CLUBS", "SEVEN", "french"))

        assertLeftEquals(service.play(request, player), "card.play.not-owned")
    }

    @Test
    fun testPlayerMustFollowSuitIfPossible() {
        val state = getState()

        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers).unnest()
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("HEARTS", "JACK", "french"))

        assertLeftEquals(service.play(request, player), "card.play.not-playable")
    }

    @Test
    fun testPlayerCanPlayValidCard() {
        val state = getState()

        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers).unnest()
        val playedCard = PlayedCard("SPADES", "SIX", "french")
        val request = PlayCardRequest(state.game.uuid.toString(), playedCard)
        val result = service.play(request, player)
        val gameState = getRightOrThrow(result)

        assertEquals(Card.from(playedCard), gameState.tricks[1].west)
        assertEquals(4, gameState.tricks.size)
    }
}