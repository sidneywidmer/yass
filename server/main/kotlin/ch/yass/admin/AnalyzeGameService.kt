package ch.yass.admin

import arrow.core.raise.Raise
import ch.yass.admin.api.AnalyzeGameStateResponse
import ch.yass.admin.api.analzye.PlayedCardWithPlayer
import ch.yass.admin.api.analzye.PlayerWithCards
import ch.yass.admin.api.analzye.PlayerWithWeise
import ch.yass.admin.api.analzye.TrickWithCards
import ch.yass.core.error.GameWithCodeNotFound
import ch.yass.core.helper.associateWithToEnum
import ch.yass.game.GameService
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Position
import ch.yass.game.dto.TotalPoints
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.sumPointsByPosition
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
        val points = pointsByPositionTotal(state.hands, state.tricks)
        val winners = getWinningTeam(points)
        val losers = getLosingTeam(points)

        return AnalyzeGameStateResponse(hands, points, state.game.uuid, winners, losers)
    }

    private fun mapHand(hand: Hand, state: GameState): ch.yass.admin.api.analzye.Hand {
        val startingPlayer = playerAtPosition(hand.startingPosition, state.seats, state.allPlayers)
        val players = state.allPlayers.map { mapPlayer(it, hand, state) }.toList()
        var details = handTricksWithPoints(listOf(hand), state.tricks)

        val weisPoints = weisPointsByPositionTotal(listOf(hand), state.tricks)
        val weise = Position.entries
            .map {
                PlayerWithWeise(
                    playerAtPosition(it, state.seats, state.allPlayers),
                    hand.weiseOf(it).map { w -> w.toWeisWithPoints(hand.trump) }
                )
            }
        val cardPoints = details.sumPointsByPosition()
        val points = Position.entries
            .associateWithToEnum { TotalPoints(cardPoints.getValue(it), weisPoints.getValue(it)) }

        val tricks = details.first().tricks.map { trickDetail ->
            val winnerPlayer = playerAtPosition(trickDetail.winner, state.seats, state.allPlayers)
            val leadPlayer = playerAtPosition(trickDetail.lead, state.seats, state.allPlayers)
            val cards = positionsOrderedWithStart(trickDetail.lead).map {
                PlayedCardWithPlayer(playerAtPosition(it, state.seats, state.allPlayers), trickDetail.trick.cardOf(it))
            }
            TrickWithCards(
                cards,
                leadPlayer,
                trickDetail.trick.cardOf(trickDetail.lead)?.suit,
                winnerPlayer,
                trickDetail.points
            )
        }

        return AnalyzeHand(hand.trump, hand.gschobe, startingPlayer, players, tricks.reversed(), points, weise)
    }

    private fun mapPlayer(player: Player, hand: Hand, state: GameState): PlayerWithCards {
        val seat = state.seats.first { it.playerId == player.id }
        val cards = hand.cardsOf(seat.position)
        return PlayerWithCards(player.uuid, player.name, cards, seat.position)
    }

}