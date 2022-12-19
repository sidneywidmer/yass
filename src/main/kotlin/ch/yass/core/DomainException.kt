package ch.yass.core

/**
 * Sometimes - e.g. from before/after handler we can't just abort a request
 * but have to fall back to throwing exceptions around :( These are handled
 * by the `exceptionHandler` function.
 */
class DomainException(val domainError: DomainError) : Exception()