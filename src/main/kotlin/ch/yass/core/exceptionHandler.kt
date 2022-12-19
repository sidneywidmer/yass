package ch.yass.core

import io.javalin.http.Context


/**
 * Transform DomainExceptions to correct responses.
 */
fun exceptionHandler(exception: DomainException, ctx: Context) {
    logger().info("DomainError '${exception.domainError.javaClass.name}' encountered: ${exception.domainError}")

    when (exception.domainError) {
        is DomainError.RequestError -> ctx.status(400).result(exception.domainError.code)
        is DomainError.OryError -> ctx.status(401).result(exception.domainError.code)
    }
}