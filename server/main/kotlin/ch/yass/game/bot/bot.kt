package ch.yass.game.bot

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Trump
import ch.yass.game.dto.Weis
import ch.yass.game.dto.db.InternalPlayer
import ch.yass.game.engine.currentHand
import ch.yass.game.engine.playerSeat
import ch.yass.game.engine.possibleWeiseWithPoints
import ch.yass.game.engine.withoutStoeckPoints

fun chooseCardForBot(botPlayer: InternalPlayer, state: GameState): PlayCandidate = getPlayCandidate(botPlayer, state)

fun chooseTrumpForBot(botPlayer: InternalPlayer, state: GameState): TrumpCandidate {
    val hand = currentHand(state.hands)
    val seat = playerSeat(botPlayer, state.seats)
    val cards = hand.cardsOf(seat.position)

    return getTrumpCandidate(cards)
}

fun chooseGschobeForBot(botPlayer: InternalPlayer, state: GameState): Gschobe {
    val candidate = chooseTrumpForBot(botPlayer, state)

    val isLowRisk = candidate.trump in setOf(Trump.CLUBS, Trump.SPADES)
    val isMediumRisk = candidate.trump in setOf(Trump.DIAMONDS, Trump.HEARTS)
    val isHighRisk = candidate.trump in setOf(Trump.OBEABE, Trump.UNEUFE)

    return when {
        isLowRisk && candidate.reason.weight >= 2 -> Gschobe.NO
        isMediumRisk && candidate.reason.weight >= 6 -> Gschobe.NO
        isHighRisk && candidate.reason.weight >= 8 -> Gschobe.NO
        candidate.reason == TrumpFallback -> Gschobe.YES
        else -> Gschobe.YES
    }
}

fun chooseWeisForBot(botPlayer: InternalPlayer, state: GameState): Weis {
    val hand = currentHand(state.hands)
    val seat = playerSeat(botPlayer, state.seats)
    val cards = hand.cardsOf(seat.position)

    return withoutStoeckPoints(possibleWeiseWithPoints(cards, hand.trump))
        .maxBy { weis -> weis.points }
        .let { Weis(it.type, it.cards) }
}
