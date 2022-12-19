package ch.yass.core

import sh.ory.ApiException

sealed class DomainError {
    data class OryError(val code: String, val exception: Throwable) : DomainError() {
        override fun toString(): String {
            return when (exception) {
                is ApiException -> exception.responseBody?.trim() ?: exception.toString()
                else -> exception.toString()
            }
        }
    }

    data class RequestError(val code: String, val validation: HashMap<String, Any?>? = null) : DomainError()
}
