package ch.yass.core.error

import ch.yass.core.helper.errorResponse
import ch.yass.core.helper.logger
import io.javalin.http.Context

/**
 * Transform DomainExceptions to correct responses.
 */
fun domainExceptionHandler(ctx: Context, exception: DomainException) {
    errorResponse(ctx, exception.domainError)
}

/**
 * Something awful happened :(
 */
fun globalExceptionHandler(exception: Exception, ctx: Context) {
    logger().error("Unhandled exception thrown.", exception)
    ctx.status(500)
}