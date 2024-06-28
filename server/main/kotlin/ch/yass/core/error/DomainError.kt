package ch.yass.core.error

import ch.yass.game.api.internal.GameState
import ch.yass.game.dto.Card
import ch.yass.game.dto.GameSettings
import ch.yass.game.dto.State
import ch.yass.game.dto.Trump
import ch.yass.game.dto.db.Player
import org.valiktor.ConstraintViolation
import sh.ory.ApiException
import sh.ory.model.Identity
import java.util.UUID

sealed interface DomainError

// Misc Errors
data class StringNoValidUUID(val string: String) : DomainError

// Request-Validation related Errors
sealed interface ValidationError : DomainError
data class JsonNotMappable(val targetClass: String?, val exception: Throwable?) : ValidationError
data class ValiktorError(val violations: Set<ConstraintViolation>) : ValidationError

// Auth related Errors
sealed interface AuthError : DomainError
data class Unauthorized(val exception: ApiException) : AuthError
data class OryIdentityWithoutName(val identity: Identity) : AuthError
data class UnauthorizedSubscription(val error: DomainError) : AuthError
data class InvalidAnonToken(val token: String) : AuthError
data class CanNotImpersonate(val player: Player, val impersonateUuid: UUID) : AuthError
data class CanNotLinkAnonAccount(val player: Player, val orySession: String) : AuthError

// Game or Game-State related Errors
sealed interface GameError : DomainError
data object GameAlreadyFull : GameError
data class GameNotFound(val uuid: String) : GameError
data class SeatNotFound(val uuid: String) : GameError
data class PlayerNotInGame(val player: Player, val state: GameState) : GameError
data class PlayerDoesNotOwnSeat(val player: Player, val seatUuid: String, val state: GameState) : GameError
data class PlayerIsLocked(val player: Player, val state: GameState) : GameError
data class PlayerDoesNotOwnCard(val player: Player, val card: Card, val state: GameState) : GameError
data class CardNotPlayable(val card: Card, val player: Player, val state: GameState) : GameError
data class InvalidState(val nextState: State, val state: GameState) : GameError
data class TrumpInvalid(val trump: Trump) : GameError
data class PlayerIsNotBot(val player: Player, val state: GameState) : GameError
data class GameSettingsMaxBots(val settings: GameSettings) : GameError
data class GameSettingsInvalidValue(val settings: GameSettings) : GameError

// Db related Errors
sealed interface DbError : DomainError
data class GameWithCodeNotFound(val code: String) : DbError