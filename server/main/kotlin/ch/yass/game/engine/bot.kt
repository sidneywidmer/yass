package ch.yass.game.engine

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.CardInHandState
import ch.yass.game.dto.Gschobe
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Player
import kotlin.random.Random

fun chooseCardForBot(botPlayer: Player, state: GameState): Card {
    val hand = currentHand(state.hands)!!
    val cards = cardsInHand(hand, botPlayer, state)
    val match = cards.shuffled().first { it.state == CardInHandState.PLAYABLE }

    return Card(match.suit, match.rank, match.skin)
}

fun chooseTrumpForBot(botPlayer: Player, state: GameState): Trump = playableTrumps().shuffled().first()

fun chooseGschobeForBot(botPlayer: Player, state: GameState): Gschobe =
    if (Random.nextBoolean()) Gschobe.YES else Gschobe.NO