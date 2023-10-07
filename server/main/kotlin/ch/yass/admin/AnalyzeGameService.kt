package ch.yass.admin

import arrow.core.raise.Raise
import ch.yass.admin.api.AnalyzeGameStateResponse2
import ch.yass.admin.api.analzye.PlayedCardWithPlayer
import ch.yass.admin.api.analzye.PlayerWithCards
import ch.yass.admin.api.analzye.TrickWithCards
import ch.yass.core.error.GameWithCodeNotFound
import ch.yass.game.GameService
import ch.yass.game.api.PlayedCard
import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Position
import ch.yass.game.dto.db.Hand
import ch.yass.game.dto.db.Player
import ch.yass.game.dto.db.Trick
import ch.yass.game.engine.currentLeadPositionOfHand
import ch.yass.game.engine.playerAtPosition
import ch.yass.game.engine.tricksOfHand
import ch.yass.admin.api.analzye.Hand as AnalyzeHand

class AnalyzeGameService(private val gameService: GameService) {

    context(Raise<GameWithCodeNotFound>)
    fun analyze(code: String): AnalyzeGameStateResponse2 {
        val state = gameService.getStateByCode(code)

        val hands = state.hands.reversed().stream().map { mapHand(it, state) }.toList()


        // how do we get from the game state to a correctly ordered list of tricks?

        return AnalyzeGameStateResponse2(hands);
    }

    private fun mapHand(hand: Hand, state: GameState): ch.yass.admin.api.analzye.Hand {
        val startingPlayer = state.allPlayers.first { it.id == hand.startingPlayerId }
        val players = state.allPlayers.map { mapPlayer(it, hand, state) }.toList()
        val tricksOfHand = tricksOfHand(state.tricks, hand)
        val tricks = tricksOfHand.map { mapTrick(it, state) }

        return AnalyzeHand(hand.trump, startingPlayer, players, tricks)
    }

    private fun mapTrick(trick: Trick, state: GameState): TrickWithCards {
        val cards = Position.entries.map {
            PlayedCardWithPlayer(
                playerAtPosition(it, state.seats, state.allPlayers)!!,
                trick.cardOf(it)
            )
        }
        Hier gehts weiter, TrickWithCards fast alles required
        return TrickWithCards(cards, null, null, null)
    }

    private fun mapPlayer(player: Player, hand: Hand, state: GameState): PlayerWithCards {
        val seat = state.seats.first { it.playerId == player.id }
        val cards = hand.cardsOf(seat.position)
        return PlayerWithCards(player.uuid, player.name, cards, seat.position)
    }

}