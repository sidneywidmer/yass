package ch.yass.integration

import arrow.core.raise.either
import ch.yass.admin.dsl.game
import ch.yass.game.GameRepository
import ch.yass.game.GameService
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Position
import ch.yass.game.dto.State
import ch.yass.game.dto.Trump
import ch.yass.game.engine.playerAtPosition
import ch.yass.game.pubsub.GameFinished
import ch.yass.game.pubsub.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance

class BotTest : Integration() {
    private val service: GameService = container.direct.instance()
    private val repo: GameRepository = container.direct.instance()

    /**
     * 4 Players in the game and the welcome hand is already played. Clubs is trump and in the first
     * trick a lot of trumps already went. Now in the second trick its WEST's turn.
     */
    private fun getState(): GameState {
        return game {
            players {
                north(name = "ueli", bot = true)
                east(name = "doris", bot = true)
                south(name = "christoph", bot = true)
                west(name = "daniela", bot = true)
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
                        trick(north = "WH", east = "WH", south = "WH", west = "WH")
                    }
                }
                hand {
                    trump(Trump.SPADES)
                    gschobe(Gschobe.NO)
                    north(cards = "C9,D7,D10,DQ,H6,HQ,HK,S9,SJ", start = true)
                    east(cards = "C7,CJ,DA,H7,H10,HA,S8,SQ,SK")
                    south(cards = "C8,C10,CQ,CA,D9,H8,H9,S7,SA")
                    west(cards = "C6,CK,D6,D8,DJ,DK,HJ,S6,S10")
                    tricks { }
                }
            }
        }
    }

    @Test
    fun testBotsPlayAFullGame() = runBlocking {
        val state = getState()

        val player = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)!!
        val playedCard = PlayedCard("CLUBS", "NINE", "french")
        val request = PlayCardRequest(state.game.uuid.toString(), playedCard)

        waitUntilEvent(
            service.scope,
            service.eventChannel,
            { current, history -> current is GameFinished },
            { either { service.play(request, player) }.onLeft { fail() } }
        )

        val currentState = repo.getState(state.game)
        assert(currentState.tricks.size == 91)
        assert(currentState.hands.size == 11)
    }

    suspend fun <T> waitUntilEvent(
        scope: CoroutineScope,
        channel: Channel<T>,
        predicate: (T, List<T>) -> Boolean,
        action: suspend () -> Unit
    ) {
        suspendCancellableCoroutine { continuation ->
            val history = mutableListOf<T>()
            scope.launch {
                channel.consumeEach { value ->
                    history.add(value)
                    if (!continuation.isCompleted && predicate(value, history.toList())) {
                        continuation.resumeWith(Result.success(Unit))
                    }
                }
            }
            scope.launch { action() }
        }
    }
}
