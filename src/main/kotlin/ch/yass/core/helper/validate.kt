package ch.yass.core.helper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import ch.yass.Yass
import ch.yass.core.error.DomainError
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.kodein.di.direct
import org.kodein.di.instance
import org.valiktor.ConstraintViolationException

inline fun <reified T> validate(json: String): Either<DomainError, T> {
    val mapper = Yass.container.direct.instance<ObjectMapper>()

    return try {
        mapper.readValue(json, T::class.java).right()
    } catch (exception: ValueInstantiationException) {
        exception.cause.toOption().fold(
            { DomainError.UnexpectedError("request.body.json.invalid").left() },
            { cause -> handleInstantiationCause(cause).left() }
        )
    } catch (exception: MissingKotlinParameterException) {
        DomainError.RequestError("body.json.parameter.missing").left()
    } catch (exception: Exception) {
        DomainError.UnexpectedError("request.body.json.invalid").left()
    }
}

fun handleInstantiationCause(cause: Throwable): DomainError {
    return when (cause) {
        is ConstraintViolationException -> DomainError.ValidationError("constraint.violations", cause.constraintViolations)
        else -> DomainError.UnexpectedError("request.body.json.invalid", cause)
    }
}