package ch.yass.core.error

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.State
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Game
import ch.yass.game.dto.db.Player
import org.valiktor.ConstraintViolation
import sh.ory.model.Identity

sealed interface DomainError


// Misc Errors
data class StringNoValidUUID(val string: String) : DomainError

// Request-Validation related Errors
sealed interface ValidationError : DomainError
data class JsonNotMappable(val targetClass: String?, val exception: Throwable?) : ValidationError
data class ValiktorError(val violations: Set<ConstraintViolation>) : ValidationError

// Auth related Errors
sealed interface AuthError : DomainError
data object SessionTokenMissing : AuthError
data class OryIdentityWithoutName(val identity: Identity) : AuthError

// Game or Game-State related Errors
sealed interface GameError : DomainError
data object GameAlreadyFull : GameError
data class PlayerIsLocked(val player: Player, val state: GameState) : GameError
data class PlayerDoesNotOwnCard(val player: Player, val card: Card, val state: GameState) : GameError
data class CardNotPlayable(val card: Card, val player: Player, val state: GameState) : GameError
data class InvalidState(val nextState: State, val state: GameState) : GameError
data class TrumpNotChosen(val state: GameState) : GameError
data class TrumpInvalid(val trump: Trump) : GameError
data class PlayerIsNotBot(val player: Player, val state: GameState) : GameError

// Db related Errors
sealed interface DbError : DomainError
data class GameWithCodeNotFound(val code: String) : DbError