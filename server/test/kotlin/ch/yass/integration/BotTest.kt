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
import ch.yass.game.dto.Trump
import ch.yass.game.engine.playerAtPosition
import ch.yass.game.engine.playerSeat
import ch.yass.game.pubsub.GameFinished
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.coroutines.resumeWithException


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

    /**
     * Broad test that let's 4 bots play against each other. The test is pretty expensive
     * since it takes around 5s to play a full game. This gives us a good overview
     * that most things should work and not throw any weird errors.
     */
    @Test
    fun testBotsPlayAFullGame() = runTest {
        val state = getState()

        val north = playerAtPosition(Position.NORTH, state.seats, state.allPlayers)
        val northSeat = playerSeat(north, state.seats)
        val playedCard = PlayedCard("CLUBS", "NINE")
        val request = PlayCardRequest(state.game.uuid.toString(), playedCard)

        waitUntilEvent(
            service.scope,
            service.eventChannel,
            { current, _ -> current.action is GameFinished && northSeat.uuid == current.seatUUID },
            { either { service.play(request, north) }.onLeft { fail() } }
        )

        val currentState = repo.getState(state.game)
        assertThat(currentState.tricks.size, equalTo(91))
        assertThat(currentState.hands.size, equalTo(11))
    }

    fun <T> waitUntilEvent(
        scope: CoroutineScope,
        channel: Channel<T>,
        predicate: (T, List<T>) -> Boolean,
        action: suspend () -> Unit
    ) = runTest {
        suspendCancellableCoroutine { continuation ->
            val history = mutableListOf<T>()

            // Monitor the scope's job for failures and if there are any resume our suspended coroutine
            scope.coroutineContext[Job]?.invokeOnCompletion { throwable ->
                throwable?.let { exception ->
                    if (!continuation.isCompleted) {
                        continuation.resumeWithException(exception)
                    }
                }
            }

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
