package ch.yass.core.helper

import arrow.core.Either
import ch.yass.core.error.DomainError.UnexpectedError
import java.util.*


fun String.toUUID(): Either<UnexpectedError, UUID> = Either.catch {
    UUID.fromString(this)
}.mapLeft { UnexpectedError("string.to.uuid.exception", it) }
