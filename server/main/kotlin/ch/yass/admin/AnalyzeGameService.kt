package ch.yass.admin

import arrow.core.raise.Raise
import ch.yass.admin.api.AnalyzeGameStateResponse
import ch.yass.admin.api.analzye.PlayedCardWithPlayer
import ch.yass.admin.api.analzye.PlayerWithCards
import ch.yass.admin.api.analzye.TrickWithCards
import ch.yass.core.error.GameWithCodeNotFound
import ch.yass.core.helper.takeWhileInclusive
import ch.yass.game.GameService
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Points
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Trick
import ch.yass.game.engine.*
import ch.yass.admin.api.analzye.Hand as AnalyzeHand

/**
 * Mash our GameState in a better format to display it in the FE.
 */
class AnalyzeGameService(private val gameService: GameService) {

    context(Raise<GameWithCodeNotFound>)
    fun analyze(code: String): AnalyzeGameStateResponse {
        val state = gameService.getStateByCode(code)
        val hands = state.hands.reversed().stream().map { mapHand(it, state) }.toList()
        val points = pointsByPositionTotal(completedHands(state.hands, state.tricks), state.tricks, state.seats)

        return AnalyzeGameStateResponse(hands, points, state.game.uuid)
    }

    private fun mapHand(hand: Hand, state: GameState): ch.yass.admin.api.analzye.Hand {
        val startingPlayer = playerAtPosition(hand.startingPosition, state.seats, state.allPlayers)!!
        val players = state.allPlayers.map { mapPlayer(it, hand, state) }.toList()
        val tricksOfHand = tricksOfHand(state.tricks, hand) // newest trick is index 0
        val tricks = tricksOfHand.map { mapTrick(it, state, tricksOfHand, hand) }
        val points = pointsByPositionTotal(listOf(hand), completeTricksOfHand(state.tricks, hand), state.seats)

        return AnalyzeHand(hand.trump, hand.gschobe, startingPlayer, players, tricks.reversed(), points)
    }

    private fun mapTrick(trick: Trick, state: GameState, tricksOfHand: List<Trick>, hand: Hand): TrickWithCards {
        val tricksUptoGivenTrick = tricksOfHand.reversed().takeWhileInclusive { it.id != trick.id }.reversed()
        val leadPosition = currentLeadPositionOfHand(hand, tricksUptoGivenTrick, state.seats)
        val leadPlayer = playerAtPosition(leadPosition, state.seats, state.allPlayers)!!
        val leadCard = trick.cardOf(leadPosition)
        val winningPosition = winningPositionOfCurrentTrick(hand, tricksUptoGivenTrick, state.seats)
        val winningPlayer = winningPosition?.let { playerAtPosition(winningPosition, state.seats, state.allPlayers) }
        val cards = positionsOrderedWithStart(leadPosition).map {
            PlayedCardWithPlayer(
                    playerAtPosition(it, state.seats, state.allPlayers)!!,
                    trick.cardOf(it)
            )
        }

        return TrickWithCards(cards, leadPlayer, leadCard?.suit, winningPlayer)
    }

    private fun mapPlayer(player: Player, hand: Hand, state: GameState): PlayerWithCards {
        val seat = state.seats.first { it.playerId == player.id }
        val cards = hand.cardsOf(seat.position)
        return PlayerWithCards(player.uuid, player.name, cards, seat.position)
    }

}