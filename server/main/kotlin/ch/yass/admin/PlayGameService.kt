package ch.yass.admin

import arrow.core.raise.Raise
import arrow.core.raise.fold
import ch.yass.admin.api.PlayGameResponse
import ch.yass.core.error.GameError
import ch.yass.core.error.GameWithCodeNotFound
import ch.yass.core.error.PlayerDoesNotOwnCard
import ch.yass.game.GameService
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.CardInHand
import ch.yass.game.dto.CardInHandState.*
import ch.yass.game.dto.SeatState
import ch.yass.game.dto.db.Seat
import ch.yass.game.engine.*

class PlayGameService(private val gameService: GameService) {

    context(Raise<GameWithCodeNotFound>)
    fun play(code: String): PlayGameResponse {
        val state = gameService.getStateByCode(code)
        val seats = state.seats.map { mapSeat(it, state) }
        val playedCards = currentTrick(state.tricks)!!.cardsByPosition()

        return PlayGameResponse(state.game.uuid, seats, playedCards)
    }

    private fun mapSeat(seat: Seat, state: GameState): SeatState {
        val player = playerAtPosition(seat.position, state.seats, state.allPlayers)!!
        val hand = currentHand(state.hands)!!
        val points = pointsByPositionTotal(completedHands(state.hands, state.tricks), state.tricks, state.seats)
        val cards = hand.cardsOf(seat.position).map {
            val cardState = fold(
                { cardIsPlayable(it, player, state) },
                { error: GameError -> if (error is PlayerDoesNotOwnCard) ALREADY_PLAYED else UNPLAYABLE },
                { PLAYABLE }
            )
            CardInHand(it.suit, it.rank, it.skin, cardState)
        }

        val nextState = nextState(state)
        val active = activePosition(state.hands, state.allPlayers, state.seats, state.tricks)

        return SeatState(seat.uuid, cards, seat.position, player, points, nextState, active, hand.trump)
    }
}