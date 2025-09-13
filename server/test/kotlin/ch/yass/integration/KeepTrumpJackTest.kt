package ch.yass.integration

import ch.yass.admin.dsl.game
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.CardInHandState
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.cardsInHand
import ch.yass.game.engine.currentHand
import ch.yass.game.engine.playerAtPosition
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KeepTrumpJackTest : Integration() {
    /**
     * 4 Players in the game and the welcome hand is already played. Clubs is trump and in the first
     * trick a lot of trumps already went. Now in the second trick its WEST's turn.
     */
    private fun getState(): GameState {
        return game {
            hands {
                hand {
                    trump(Trump.SPADES)
                    gschobe(Gschobe.NO)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,HA,SJ")
                    east(cards = "C7,CJ,DA,H7,H10,S8,S9,SQ,SK")
                    south(cards = "C8,C10,CQ,CA,D9,H8,H9,S7,SA")
                    west(cards = "C6,CK,D6,D8,DJ,DK,HJ,S6,S10", start = true)
                    tricks {
                        trick(north = null, east = null, south = null, west = "S10")
                    }
                }
            }
        }
    }

    @Test
    fun testPlayerMustNotPlayTrumpJack() {
        val state = getState()

        val player = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val foo = cardsInHand(currentHand(state.hands), player, state)
        val playableCards = foo.filter { it.state == CardInHandState.PLAYABLE }

        // When only Trump Jack -> all cards are playable
        assertTrue { playableCards.size == 9 }
    }

}