package ch.yass.unit

import ch.yass.game.dto.GameKind
import ch.yass.game.dto.GameSettings
import ch.yass.game.dto.GameStatus
import ch.yass.game.dto.Position
import ch.yass.game.dto.SeatStatus
import ch.yass.game.dto.WinningConditionType
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.InternalPlayer
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.gameIsCancelable
import ch.yass.game.engine.gameIsCanceled
import ch.yass.game.engine.playerCreatedGame
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class CancelChecksTest {

    private val now: LocalDateTime = LocalDateTime.now()

    private fun player(id: Int) = InternalPlayer(id, UUID.randomUUID(), null, "player-$id", false, null, now, now)

    private fun seat(id: Int, playerId: Int?, position: Position, status: SeatStatus) =
        Seat(id, UUID.randomUUID(), now, now, playerId, 1, position, status, null, now)

    private fun game(kind: GameKind, status: GameStatus) = Game(
        1,
        UUID.randomUUID(),
        now,
        now,
        "ABCDE",
        42L,
        GameSettings(false, false, false, false, WinningConditionType.HANDS, 10),
        status,
        kind
    )

    /**
     * The creator is inferred as the first human to be seated, so bots seated upfront must not win the
     * comparison even though they hold the lower seat ids.
     */
    @Test
    fun testPlayerCreatedGameIgnoresBotsSeatedFirst() {
        val creator = player(10)
        val joiner = player(11)
        val seats = listOf(
            seat(1, null, Position.NORTH, SeatStatus.BOT),
            seat(2, null, Position.EAST, SeatStatus.BOT),
            seat(3, creator.id, Position.SOUTH, SeatStatus.CONNECTED),
            seat(4, joiner.id, Position.WEST, SeatStatus.CONNECTED)
        )

        assertTrue(playerCreatedGame(creator, seats))
        assertFalse(playerCreatedGame(joiner, seats))
    }

    /**
     * A creator that dropped out keeps their seat, so a DISCONNECTED status must not hand the game over
     * to whoever joined after them.
     */
    @Test
    fun testPlayerCreatedGameSurvivesDisconnect() {
        val creator = player(10)
        val joiner = player(11)
        val seats = listOf(
            seat(1, creator.id, Position.NORTH, SeatStatus.DISCONNECTED),
            seat(2, joiner.id, Position.EAST, SeatStatus.CONNECTED)
        )

        assertTrue(playerCreatedGame(creator, seats))
        assertFalse(playerCreatedGame(joiner, seats))
    }

    @Test
    fun testOnlyRunningCustomGamesAreCancelable() {
        assertTrue(gameIsCancelable(game(GameKind.CUSTOM, GameStatus.RUNNING)))

        assertFalse(gameIsCancelable(game(GameKind.DAILY, GameStatus.RUNNING)))
        assertFalse(gameIsCancelable(game(GameKind.CUSTOM, GameStatus.FINISHED)))
        assertFalse(gameIsCancelable(game(GameKind.CUSTOM, GameStatus.CANCELED)))
    }

    @Test
    fun testGameIsCanceled() {
        assertTrue(gameIsCanceled(game(GameKind.CUSTOM, GameStatus.CANCELED)))

        assertFalse(gameIsCanceled(game(GameKind.CUSTOM, GameStatus.RUNNING)))
        assertFalse(gameIsCanceled(game(GameKind.CUSTOM, GameStatus.FINISHED)))
    }
}
