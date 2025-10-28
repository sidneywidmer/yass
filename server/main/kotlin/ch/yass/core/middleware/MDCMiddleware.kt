package ch.yass.core.middleware

import ch.yass.core.contract.MDCAttributes.TRACE_ID
import ch.yass.core.contract.Middleware
import io.javalin.http.Context
import org.slf4j.MDC
import java.util.*

class MDCMiddleware : Middleware {
    override fun before(ctx: Context) {
        MDC.put(TRACE_ID.value, UUID.randomUUID().toString())
    }

    override fun after(ctx: Context) {
        ctx.header("X-Request-UUID", MDC.get(TRACE_ID.value))
        MDC.remove(TRACE_ID.value)
    }
}