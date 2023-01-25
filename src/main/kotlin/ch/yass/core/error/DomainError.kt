package ch.yass.core.error

import org.valiktor.ConstraintViolation
import sh.ory.ApiException

/**
 * Overall error class for the whole project. Whenever something can/goes wrong some
 * kind of DomainError needs to be returned right. We're avoiding to throw
 * exceptions whenever possible.
 */
sealed class DomainError {
    // Related to the ory service. If something goes south this error wraps ory's exception.
    // In case it's an ApiException we're also cleaning up the to string a little to avoid
    // the whole stacktrace. This usually means the user is not correctly authorized
    // -> results in a 401
    data class OryError(val code: String, val exception: Throwable) : DomainError() {
        override fun toString(): String {
            return when (exception) {
                is ApiException -> exception.responseBody?.trim() ?: exception.toString()
                else -> exception.toString()
            }
        }
    }

    // Valiktor validation related errors. The list of ConstraintViolations gets cleaned up
    // before we return the errorResponse -> results in a 422
    data class ValidationError(val code: String, val payload: Set<ConstraintViolation>? = null) : DomainError()

    // Valiktor validation related errors. The list of ConstraintViolations gets cleaned up
    // before we return the errorResponse -> results in a 422
    data class ValiktorError(val code: String, val payload: Set<ConstraintViolation>) : DomainError()

    // Something, anything related to a malformed request. Be either missing but required properties, missing
    // headers, malformed json or whatever -> results in a 400
    data class RequestError(val code: String, val payload: HashMap<String, Any?>? = null) : DomainError()

    // Something wrong with the DB? Did you expect a result but didn't get anything? DbError will take
    // care of it -> results in a 500 but _should_ never bubble up to the errorResponse
    data class DbError(val code: String, val exception: Throwable? = null) : DomainError()

    // BAM! - that was unexpected, right? Used liberally all over the code as an easy way out and
    // obviously shouldn't bubble up to the errorResponse -> results in a 500
    data class UnexpectedError(val code: String, val exception: Throwable? = null) : DomainError()
}
