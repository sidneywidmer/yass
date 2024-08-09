package ch.yass.integration

import arrow.core.raise.fold
import ch.yass.Yass
import ch.yass.admin.dsl.game
import ch.yass.core.error.GameNotFound
import ch.yass.core.error.PlayerDoesNotOwnCard
import ch.yass.core.error.PlayerIsLocked
import ch.yass.game.GameService
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.playerAtPosition
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance

class WelcomeHandTest : Integration() {
    private val service: GameService = Yass.container.direct.instance()

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
                    gschobe(Gschobe.NO)
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

        val player = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)!!
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "HELLO", "french"))

        fold(
            { service.play(request, player) },
            { assertTrue(it is PlayerIsLocked) },
            { fail() }
        )
    }

    @Test
    fun testCantPlayNotOwnedCard() {
        val state = getState()

        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers)!!
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("CLUBS", "TEN", "french"))

        fold(
            { service.play(request, player) },
            { assertTrue(it is PlayerDoesNotOwnCard) },
            { fail() }
        )
    }

    @Test
    fun testNewHandCreated() {
        val state = getState()

        // WEST plays the last missing card in this trick -> should result in a new hand
        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers)!!
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "HELLO", "french"))

        // TODO: don't launch in global scope, manage this somehow
        fold(
            { service.play(request, player) },
            { fail() },
            {
                assertEquals(2, it.hands.count())
                assertEquals(2, it.tricks.count())
            }
        )
    }

    @Test
    fun testPlayCardInInvalidGame() {
        val state = getState()
        val unknonwId = "b562227b-cc63-48cc-919e-9a115bbf7c6e"

        val player = playerAtPosition(Position.WEST, state.seats, state.allPlayers)!!
        val request = PlayCardRequest(unknonwId, PlayedCard("WELCOME", "HELLO", "french"))

        fold(
            { service.play(request, player) },
            {
                val error = it as GameNotFound
                assertEquals(error.uuid, unknonwId)
            },
            {
                fail()
            }
        )
    }
}