package ch.yass.core.helper

import arrow.core.raise.Raise
import ch.yass.Yass
import ch.yass.core.error.JsonNotMappable
import ch.yass.core.error.ValidationError
import ch.yass.core.error.ValiktorError
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import org.kodein.di.direct
import org.kodein.di.instance
import org.valiktor.ConstraintViolationException

/**
 * Try to map given string to object of type T. If this object has a valikor init
 * validation defined this will automatically get triggered by jackson. If
 * anything goes wrong, an appropriate DomainError will be returned.
 */
context(Raise<ValidationError>)
inline fun <reified T> validate(json: String): T {
    return try {
        jackson().readValue(json, T::class.java)
    } catch (exception: ValueInstantiationException) {
        when (exception.cause) {
            is ConstraintViolationException -> raise(ValiktorError((exception.cause as ConstraintViolationException).constraintViolations))
            else -> raise(JsonNotMappable(null, exception.cause))
        }
    } catch (exception: Exception) {
        raise(JsonNotMappable(T::class.java.toString(), exception))
    }
}