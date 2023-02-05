package ch.yass

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import ch.yass.core.error.DomainError
import junit.framework.AssertionFailedError

fun <L, R> assertLeftEquals(either: Either<L, R>, string: String): Boolean {
    val value = either.fold({ it }, { throw AssertionFailedError("Expected a Either.Left, got Either.Right") })

    return when (value) {
        is DomainError.OryError -> value.code == string
        is DomainError.ValiktorError -> value.code == string
        is DomainError.ValidationError -> value.code == string
        is DomainError.RequestError -> value.code == string
        is DomainError.UnexpectedError -> value.message == string
        else -> throw AssertionFailedError("Unknown DomainError")
    }
}

fun <L, R> getRightOrThrow(either: Either<L, R>): R {
    return either.fold({ throw AssertionFailedError("Expected a Either.Right, got Either.Left: $either") }, { it })
}

fun <T : Any> Option<T>.unnest(): T =
    when (this) {
        is Some -> value
        is None -> throw AssertionFailedError("Expected Some, got None")
    }
