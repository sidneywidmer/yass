package ch.yass.core.middleware

import ch.yass.core.contract.CtxAttributes
import ch.yass.core.contract.Middleware
import ch.yass.core.helper.correlationId
import ch.yass.core.helper.logger
import io.javalin.http.Context

class RequestLoggerMiddleware : Middleware {

    override fun before(ctx: Context) {
        val correlationId = correlationId()
        ctx.attribute(CtxAttributes.REQUEST_CORRELATION.name, correlationId)

        val logMessage = "Request[$correlationId]: ${ctx.method()} ${ctx.fullUrl()} | Body: ${ctx.body()}"
        logger().info(logMessage)
    }

    override fun after(ctx: Context) {
        val correlationId = ctx.attribute<String>(CtxAttributes.REQUEST_CORRELATION.name)
        val logMessage = "Response[$correlationId]: ${ctx.status().name} | Body: ${ctx.result()}"
        logger().info(logMessage)
    }

}
