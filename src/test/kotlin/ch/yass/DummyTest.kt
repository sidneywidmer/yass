package ch.yass

import org.junit.Test
import ch.yass.dsl.game
import ch.yass.game.GameService
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.dto.Position
import ch.yass.game.dto.Trump
import ch.yass.game.engine.playerAtPosition
import org.kodein.di.direct
import org.kodein.di.instance

class DummyTest : BaseTest() {

    @Test
    fun testBasicDSL() {
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

        val player = playerAtPosition(Position.WEST, state)
        val request = PlayCardRequest(state.game.uuid.toString(), PlayedCard("WELCOME", "HELLO", "french"))
        val result = service.play(request, player)

        val foo = "bar"
    }

}