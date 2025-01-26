package ch.yass.integration.helper

import ch.yass.core.pubsub.Action
import ch.yass.game.dto.*
import ch.yass.game.dto.db.InternalPlayer
import ch.yass.game.pubsub.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertTrue
import kotlin.reflect.KClass

class ActionAssertions(private val actions: List<Action>) {
    fun hasCount(action: KClass<out Action>, count: Int) {
        val matchingActions = actions.count { it::class == action }
        assertThat(matchingActions, equalTo(count))
    }

    fun hasState(state: State) {
        assertTrue(actions.any { it is UpdateState && it.state == state })
    }

    fun hasPlayedCard(suit: Suit, rank: Rank) {
        assertTrue(
            actions.any { action ->
                (action as? CardPlayed)?.card?.let {
                    it.suit == suit && it.rank == rank
                } == true
            }
        )
    }

    fun hasWeis(type: WeisType, points: Int) {
        assertTrue(
            actions.any { action ->
                (action as? ShowWeis)?.weis?.let {
                    it.type == type && it.points == points
                } == true
            }
        )
    }

    fun hasTrump(trump: Trump) {
        assertTrue(actions.any { it is UpdateTrump && it.trump == trump })
    }

    fun hasWinner(player: InternalPlayer) {
        assertTrue(actions.any { it is GameFinished && it.winners.contains(Player.from(player)) })
    }
}
