package ch.yass.game.engine

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.*
import ch.yass.game.dto.db.Player
import kotlin.random.Random

fun botId(position: Position): Int {
    return when (position) {
        Position.NORTH -> -1
        Position.EAST -> -2
        Position.SOUTH -> -3
        Position.WEST -> -4
    }
}

fun chooseCardForBot(botPlayer: Player, state: GameState): Card {
    val hand = currentHand(state.hands)!!
    val cards = cardsInHand(hand, botPlayer, state)
    val match = cards.shuffled().first { it.state == CardInHandState.PLAYABLE }

    return Card(match.suit, match.rank, match.skin)
}

fun chooseTrumpForBot(botPlayer: Player, state: GameState): Trump = playableTrumps().shuffled().first()

fun chooseGschobeForBot(botPlayer: Player, state: GameState): Gschobe =
    if (Random.nextBoolean()) Gschobe.YES else Gschobe.NO

fun chooseWeisForBot(botPlayer: Player, state: GameState): Weis {
    val hand = currentHand(state.hands)!!
    val seat = playerSeat(botPlayer, state.seats)
    val cards = hand.cardsOf(seat.position)

    return withoutStoeckPoints(possibleWeiseWithPoints(cards, hand.trump!!))
        .maxBy { weis -> weis.points }
        .let { Weis(it.type, it.cards) }
}
