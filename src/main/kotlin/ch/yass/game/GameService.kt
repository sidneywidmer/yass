package ch.yass.game

import arrow.core.Either
import arrow.core.continuations.either
import ch.yass.core.error.DomainError
import ch.yass.core.error.DomainError.*
import ch.yass.game.api.ChooseTrumpRequest
import ch.yass.game.api.JoinGameRequest
import ch.yass.game.api.PlayCardRequest
import ch.yass.game.api.PlayedCard
import ch.yass.game.api.internal.GameState
import ch.yass.game.api.internal.NewHand
import ch.yass.game.dto.Card
import ch.yass.game.dto.State
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.Player
import ch.yass.game.engine.*

class GameService(private val repo: GameRepository) {
    fun join(request: JoinGameRequest, player: Player): Either<DomainError, GameState> = either.eager {
        val maybeGame = repo.getByCode(request.code).bind()
        val game = maybeGame.toEither { ValidationError("game.take-a-seat.empty") }.bind()

        repo.takeASeat(game, player).bind()
        repo.getState(game).bind()
    }

    fun play(request: PlayCardRequest, player: Player): Either<DomainError, GameState> = either.eager {
        val game = repo.getByUUID(request.game).bind()
        val state = repo.getState(game).bind()
        val playedCard = Card.from(request.card)
        val nextState = nextState(state)

        ensure(listOf(State.PLAY_CARD, State.PLAY_CARD_BOT).contains(nextState)) {
            UnexpectedError("invalid game state, should have been ${State.PLAY_CARD} or ${State.PLAY_CARD_BOT} but was $nextState")
        }
        ensure(playerHasTurn(player, state).bind()) { ValidationError("play.player.locked") }
        ensure(playerOwnsCard(player, playedCard, state).bind()) { ValidationError("play.not-owned") }
        ensure(cardIsPlayable(playedCard, player, state).bind()) { ValidationError("play.not-playable") }

        val currentTrick = currentTrick(state.tricks).bind { UnexpectedError("current trick is empty") }
        val playerSeat = playerSeat(player, state.seats).bind { UnexpectedError("player seat is empty") }

        repo.playCard(playedCard, currentTrick, playerSeat).bind()
        gameLoop(game)
        repo.getState(game).bind()
    }

    fun trump(request: ChooseTrumpRequest, player: Player): Either<DomainError, GameState> = either.eager {
        val game = repo.getByUUID(request.game).bind()
        val state = repo.getState(game).bind()
        val chosenTrump = Trump.valueOf(request.trump)
        val nextState = nextState(state)
        val currentHand = currentHand(state.hands).bind { UnexpectedError("current hand is empty") }

        ensure(listOf(State.TRUMP, State.TRUMP_BOT).contains(nextState)) {
            UnexpectedError("invalid game state, should have been ${State.TRUMP} or ${State.TRUMP_BOT} but was $nextState")
        }
        ensure(playerHasTurn(player, state).bind()) { ValidationError("trump.player.wrong") }
        ensure(trumps().contains(chosenTrump)) { ValidationError("trump.invalid") }

        repo.chooseTrump(chosenTrump, currentHand).bind()
        gameLoop(game)
        repo.getState(game).bind()
    }

    private fun gameLoop(game: Game): Either<DomainError, Unit> = either.eager {
        do {
            val updatedState = repo.getState(game).bind()
            val currentHand = currentHand(updatedState.hands).bind { UnexpectedError("current hand is empty") }
            val nextStateLoop = nextState(updatedState)

            when (nextStateLoop) {
                State.FINISHED -> {}
                State.PLAY_CARD -> {}
                State.TRUMP -> {}
                State.PLAY_CARD_BOT -> playAsBot(updatedState).bind()
                State.TRUMP_BOT -> trumpAsBot(updatedState).bind()
                State.NEW_TRICK -> repo.createTrick(currentHand).bind()
                State.NEW_HAND -> {
                    val startingPlayer = nextHandStartingPlayer(
                        updatedState.hands,
                        updatedState.allPlayers,
                        updatedState.seats
                    ).bind { UnexpectedError("starting player is empty") }
                    val newHand = repo.createHand(NewHand(game, startingPlayer, randomHand())).bind()
                    repo.createTrick(newHand).bind()
                }
            }
        } while (listOf(State.NEW_TRICK, State.NEW_HAND).contains(nextStateLoop))
    }

    private fun trumpAsBot(state: GameState): Either<DomainError, GameState> = either.eager {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)
            .bind { UnexpectedError("player is not a bot") }

        if (!botPlayer.bot) {
            shift<UnexpectedError>(UnexpectedError("player ${botPlayer.uuid} is not a bot"))
        }

        // TODO: Get a good trump to choose
        val trump = chooseTrumpForBot(botPlayer, state).bind()
        val request = ChooseTrumpRequest(state.game.uuid.toString(), trump.name)

        trump(request, botPlayer).bind()
    }

    private fun playAsBot(state: GameState): Either<DomainError, GameState> = either.eager {
        val botPlayer = activePlayer(state.hands, state.allPlayers, state.seats, state.tricks)
            .bind { UnexpectedError("player is not a bot") }

        if (!botPlayer.bot) {
            shift<UnexpectedError>(UnexpectedError("player ${botPlayer.uuid} is not a bot"))
        }

        // TODO: Get a good card to play
        val card = chooseCardForBot(botPlayer, state).bind()

        val request = PlayCardRequest(
            state.game.uuid.toString(),
            PlayedCard(card.suit.toString(), card.rank.toString(), card.skin)
        )

        // Possible recursion? Naahhh, never...
        play(request, botPlayer).bind()
    }
}