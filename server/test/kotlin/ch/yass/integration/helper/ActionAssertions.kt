package ch.yass.integration.helper

import ch.yass.core.pubsub.Action
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Player
import ch.yass.game.pubsub.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import kotlin.reflect.KClass

class ActionAssertions(private val actions: List<Action>) {
    fun hasCount(action: KClass<out Action>, count: Int) {
        val matchingActions = actions.count { it::class == action }
        assertThat(
            "Expected ${count}x ${action::class.simpleName} actions",
            matchingActions,
            equalTo(count)
        )
    }

    fun hasState(state: State) {
        assertThat(
            "Expected UpdateState action with state $state",
            actions.any { it is UpdateState && it.state == state }
        )
    }

    fun hasPlayedCard(suit: Suit, rank: Rank) {
        assertThat(
            "Expected CardPlayed action with $rank of $suit",
            actions.any { action ->
                (action as? CardPlayed)?.card?.let {
                    it.suit == suit && it.rank == rank
                } == true
            }
        )
    }

    fun hasWeis(type: WeisType, points: Int) {
        assertThat(
            "Expected ShowWeis action with $type worth $points points",
            actions.any { action ->
                (action as? ShowWeis)?.weis?.let {
                    it.type == type && it.points == points
                } == true
            }
        )
    }

    fun hasTrump(trump: Trump) {
        assertThat(
            "Expected UpdateTrump action with trump $trump",
            actions.any { it is UpdateTrump && it.trump == trump }
        )
    }

    fun hasWinner(player: Player) {
        assertThat(
            "Expected GameFinished action with winner ${player.name}",
            actions.any { it is GameFinished && it.winners.contains(player) }
        )
    }
}
