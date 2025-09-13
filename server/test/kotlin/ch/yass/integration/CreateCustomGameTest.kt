package ch.yass.integration

import arrow.core.raise.recover
import ch.yass.game.GameRepository
import ch.yass.game.GameService
import ch.yass.game.PlayerService
import ch.yass.game.api.CreateCustomGameRequest
import ch.yass.game.api.internal.NewAnonPlayer
import ch.yass.game.dto.Position
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.kodein.di.direct
import org.kodein.di.instance


class CreateCustomGameTest : Integration() {
    private val gameService: GameService = container.direct.instance()
    private val playerService: PlayerService = container.direct.instance()
    private val repo: GameRepository = container.direct.instance()

    @Test
    fun testNormalCustomGame() {
        val player = playerService.create(NewAnonPlayer("Fooo", "HASHED_TOKEN"))
        val request = CreateCustomGameRequest(false, true, true, true, "POINTS", 2500)

        val gameCode = recover({ gameService.create(request, player) }) { fail() }
        val game = recover({ repo.getByCode(gameCode) }) { fail() }
        val state = repo.getState(game)

        assertThat(state.seats.first { it.position == Position.NORTH }.playerId, equalTo(player.id))
        assertThat(state.seats.size, equalTo(4))
    }
}
