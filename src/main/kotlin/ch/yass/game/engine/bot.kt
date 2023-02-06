package ch.yass.game.engine

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.toOption
import ch.yass.core.error.DomainError.*
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.db.Player

fun chooseCardForBot(botPlayer: Player, state: GameState): Either<UnexpectedError, Card> = either.eager {
    val cards = unplayedCardsOfPlayer(
        botPlayer,
        state.hands,
        state.seats,
        state.tricks
    ).bind { UnexpectedError("could not find unplayed cards of bot") }

    cards.shuffled()
        .firstOrNull { cardIsPlayable(it, botPlayer, state).bind() }
        .toOption()
        .bind { UnexpectedError("player ${botPlayer.uuid} has no valid card left to play") }
}