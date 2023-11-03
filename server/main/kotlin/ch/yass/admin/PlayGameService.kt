package ch.yass.admin

import arrow.core.raise.Raise
import ch.yass.admin.api.PlayGameResponse
import ch.yass.admin.api.play.PlayGameSeat
import ch.yass.core.error.GameWithCodeNotFound
import ch.yass.game.GameService
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.CardInHand
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.cardIsPlayable
import ch.yass.game.engine.currentHand
import ch.yass.game.engine.currentTrick
import ch.yass.game.engine.playerAtPosition

class PlayGameService(private val gameService: GameService) {

    context(Raise<GameWithCodeNotFound>)
    fun play(code: String): PlayGameResponse {
        val state = gameService.getStateByCode(code)
        val seats = state.seats.map { mapSeat(it, state) }
        val playedCards = currentTrick(state.tricks)!!.cardsByPosition()

        return PlayGameResponse(state.game.uuid, seats, playedCards)

    }

    private fun mapSeat(seat: Seat, state: GameState): PlayGameSeat {
        val player = playerAtPosition(seat.position, state.seats, state.allPlayers)!!
        val hand = currentHand(state.hands)!!
        val cards = hand.cardsOf(seat.position).map {
            CardInHand(it.suit, it.rank, it.skin, cardIsPlayable(it, player, state))
        }

        return PlayGameSeat(seat.uuid, cards, seat.position, player)
    }
}