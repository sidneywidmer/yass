package ch.yass.core.helper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import ch.yass.Yass
import ch.yass.core.error.DomainError.*
import ch.yass.core.error.DomainError
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.kodein.di.direct
import org.kodein.di.instance
import org.valiktor.ConstraintViolationException

/**
 * Try to map given string to object of type T. If this object as a valikor init
 * validation defined this will automatically get triggered by jackson. If
 * anything goes wrong, an appropriate DomainError will be returned.
 */
inline fun <reified T> validate(json: String): Either<DomainError, T> {
    val mapper = Yass.container.direct.instance<ObjectMapper>()

    return try {
        mapper.readValue(json, T::class.java).right()
    } catch (exception: ValueInstantiationException) {
        exception.cause.toOption().fold(
            { UnexpectedError("request.body.json.invalid").left() },
            { cause -> handleInstantiationCause(cause).left() }
        )
    } catch (exception: MissingKotlinParameterException) {
        RequestError("body.json.parameter.missing").left()
    } catch (exception: Exception) {
        UnexpectedError("request.body.json.invalid").left()
    }
}

fun handleInstantiationCause(cause: Throwable): DomainError {
    return when (cause) {
        is ConstraintViolationException -> ValiktorError(
            "constraint.violations",
            cause.constraintViolations
        )

        else -> UnexpectedError("request.body.json.invalid", cause)
    }
}