package ch.yass.core.error

import ch.yass.core.helper.logger
import io.javalin.http.Context

/**
 * Transform DomainExceptions to correct responses.
 */
fun domainExceptionHandler(exception: DomainException, ctx: Context) {
    logger().info("DomainError '${exception.domainError.javaClass.name}' encountered: ${exception.domainError}")

    when (exception.domainError) {
        is DomainError.RequestError -> ctx.status(400).json(object {
            val code = exception.domainError.code
            val payload = exception.domainError.payload
        })

        is DomainError.OryError -> ctx.status(401).json(object {
            val code = exception.domainError.code
            val payload = null
        })

        is DomainError.DbError -> ctx.status(500).json(object {
            val code = exception.domainError.code
            val payload = null
        })

        is DomainError.UnexpectedError -> ctx.status(500).json(object {
            val code = exception.domainError.code
            val payload = null
        })

        is DomainError.ValidationError -> ctx.status(422).json(object {
            val code = exception.domainError.code
            val payload = exception.domainError.payload.map { "${it.property}: ${it.constraint.name}" }
        })
    }
}

/**
 * Something awful happened - so much for "no side effects" :D
 */
fun globalExceptionHandler(exception: Exception, ctx: Context) {
    logger().error("Unhandled exception thrown.", exception)
    ctx.status(500).json(object {
        val code = "unexpected"
        val payload = null
    })
}