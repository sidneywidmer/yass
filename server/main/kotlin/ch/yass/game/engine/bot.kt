package ch.yass.game.engine

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Player

fun chooseCardForBot(botPlayer: Player, state: GameState): Card {
    val cards = unplayedCardsOfPlayer(botPlayer, state.hands, state.seats, state.tricks)

    return cards.shuffled().first { cardIsPlayable(it, botPlayer, state) }
}

fun chooseTrumpForBot(botPlayer: Player, state: GameState): Trump = regularTrumps().shuffled().first()
