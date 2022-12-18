package ch.yass.core

sealed class DomainError {
    data class OryError(val msg: String, val exception: Throwable) : DomainError()
    data class RequestError(val msg: String, val validation: HashMap<String, Any?>? = null) : DomainError()
    data class TbdError(val msg: String) : DomainError()
}
