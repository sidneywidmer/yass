package ch.yass.core.helper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.yass.Yass
import ch.yass.core.error.DomainError
import ch.yass.core.error.DomainError.ValidationError
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import org.kodein.di.direct
import org.kodein.di.instance
import org.valiktor.ConstraintViolationException

inline fun <reified T> validate(json: String): Either<DomainError, T> {
    val mapper = Yass.container.direct.instance<ObjectMapper>()

    return try {
        mapper.readValue(json, T::class.java).right()
    } catch (exception: ValueInstantiationException) {
        when (exception.cause::class) {
            ConstraintViolationException::class -> {
                ValidationError("body.json.invalid", exception.cause.constraintViolations).left()
            }
        }

    } catch (exception: Exception) {
        DomainError.RequestError("body.json.invalid").left()
    }
}

//inline fun <reified T> jsonDecode(json: String): Either<DomainError.RequestError, T> {
//    val mapper = Yass.container.direct.instance<ObjectMapper>()
//
//    // TODO: Sep function, can throw
//    val foo = mapper.readValue(json, T::class.java)
//
//
//}