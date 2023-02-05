package ch.yass

import org.junit.Test
import ch.yass.dsl.game
import ch.yass.game.GameService
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.playerAtPosition
import junit.framework.TestCase.assertEquals
import org.kodein.di.direct
import org.kodein.di.instance

class WelcomeHandTest : BaseTest() {
    private val service: GameService = container.direct.instance()

    /**
     * Pretty basic setup, 4 players already joined the game and the welcome hand is dealt.
     * Ensure only the correct remaining player can play a card and the rest is blocked.
     * When the last card of the trick is played a new hand should be generated.
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
                        trick(north = "WH", east = "WH", south = "WH", west = null)
                    }
                }
            }
        }
    }

    @Test
    fun testPlayerLocked() {
        val state = getState()

        val player = playerAtPosition(Position.NORTH, state)
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "HELLO", "french"))

        assertLeftEquals(service.play(request, player), "card.play.player.locked")
    }

    @Test
    fun testCantPlayNotOwnedCard() {
        val state = getState()

        val player = playerAtPosition(Position.WEST, state)
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("CLUBS", "TEN", "french"))

        assertLeftEquals(service.play(request, player), "card.play.not-owned")
    }


    @Test
    fun testNewHandCreated() {
        val state = getState()

        val player = playerAtPosition(Position.WEST, state)
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "HELLO", "french"))

        val result = service.play(request, player)
        val gameState = getRightOrThrow(result)

        assertEquals(2, gameState.hands.count())
        assertEquals(2, gameState.tricks.count())
    }

    @Test
    fun testPlayCardInInvalidGame() {
        val state = getState()

        val player = playerAtPosition(Position.WEST, state)
        val request = PlayCardRequest("b562227b-cc63-48cc-919e-9a115bbf7c6e", PlayedCard("WELCOME", "HELLO", "french"))

        assertLeftEquals(service.play(request, player), "game.get-by-uuid.uuid.invalid")
    }
}