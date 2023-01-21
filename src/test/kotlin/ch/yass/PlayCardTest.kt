package ch.yass

import arrow.core.getOrHandle
import arrow.core.leftWiden
import org.junit.Test
import ch.yass.dsl.game
import ch.yass.game.GameService
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.playerAtPosition
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.kodein.di.direct
import org.kodein.di.instance

class PlayCardTest : BaseTest() {

    /**
     * Pretty basic setup, 4 players already joined the game and the welcome hand is dealt.
     * Ensure only the correct remaining player can play a card and the rest is blocked.
     * When the last card of the trick is played a new hand should be generated.
     */
    @Test
    fun welcomeHand() {
        val service: GameService = container.direct.instance()
        val state = game {
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
                        trick(north = "WH", east = "WH", south = "WH", west = null)
                    }
                }
            }
        }

        val north = playerAtPosition(Position.NORTH, state)
        val west = playerAtPosition(Position.WEST, state)
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "HELLO", "french"))

        val resultNorth = service.play(request, north)
        assertTrue(resultNorth.isLeft())
        // TODO: assert code with some new helper function
        //       also check some of the possible codes/cases

        val resultWest = service.play(request, west)
        assertTrue(resultWest.isRight())
        // TODO: Check if new hand was created
    }
}