package ch.yass.game.bot

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.CardInHand
import ch.yass.game.dto.CardInHandState
import ch.yass.game.dto.Position
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.InternalPlayer
import ch.yass.game.dto.db.Trick
import ch.yass.game.engine.*

data class PlayCandidate(val card: Card, val reasons: List<PlayReason>)

data class PlayParams(
    val hand: Hand,
    val trick: Trick,
    val playable: List<CardInHand>,
    val lead: Position,
    val state: GameState
)

sealed interface PlayReason {
    val weight: Int
    fun check(card: Card, params: PlayParams): Boolean
}

object CanWinTrick : PlayReason {
    override val weight = 8
    override fun check(card: Card, params: PlayParams): Boolean {
        if (params.trick.cards().size != 3) return false

        val suitLed = params.trick.cardOf(params.lead)!!.suit // can't be null, there are 3 cards in trick
        val cards = params.trick.cards() + card // let's add our card in question
        val winningCard = cards
            .filter { it.suit == suitLed || it.suit == params.hand.trump.toSuit() }
            .maxBy { cardValue(it, params.hand.trump) }

        return winningCard == card
    }
}

// In case no other PlayReason matches, you'll get this one
object PlayFallback : PlayReason {
    override val weight = 0
    override fun check(card: Card, params: PlayParams) = true
}

val playReasons = listOf(
    CanWinTrick,
    PlayFallback,
)

fun getPlayCandidate(player: InternalPlayer, state: GameState): PlayCandidate {
    val hand = currentHand(state.hands)
    val trick = currentTrick(state.tricks)
    val cards = cardsInHand(hand, player, state)
    val playable = cards.filter { it.state == CardInHandState.PLAYABLE }
    val lead = currentLeadPositionOfHand(hand, tricksOfHand(state.tricks, hand), state.seats)

    return playable
        .map { card ->
            val params = PlayParams(hand, trick, playable, lead, state)
            val matchingReasons = playReasons.filter { it.check(Card.from(card), params) }
            PlayCandidate(card = Card.from(card), reasons = matchingReasons)
        }
        .maxBy { candidate -> candidate.reasons.sumOf { it.weight } }
}