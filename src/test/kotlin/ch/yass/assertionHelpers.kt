package ch.yass

import arrow.core.Either
import ch.yass.core.error.DomainError
import junit.framework.AssertionFailedError

fun <L, R> assertLeftCodeEquals(either: Either<L, R>, code: String): Boolean {
    val value = either.fold({ it }, { throw AssertionFailedError("Expected a Either.Left, got Either.Right") })

    return when (value) {
        is DomainError.DbError -> value.code == code
        is DomainError.OryError -> value.code == code
        is DomainError.ValiktorError -> value.code == code
        is DomainError.ValidationError -> value.code == code
        is DomainError.RequestError -> value.code == code
        is DomainError.DbError -> value.code == code
        is DomainError.UnexpectedError -> value.code == code
        else -> throw AssertionFailedError("Unknown DomainError")
    }
}

fun <L, R> getRightOrThrow(either: Either<L, R>): R {
    return either.fold({ throw AssertionFailedError("Expected a Either.Right, got Either.Left") }, { it })
}
