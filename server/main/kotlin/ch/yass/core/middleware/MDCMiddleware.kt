package ch.yass.core.middleware

import ch.yass.core.contract.MDCAttributes.TRACE_ID
import ch.yass.core.contract.Middleware
import ch.yass.core.helper.config
import io.javalin.http.Context
import org.slf4j.MDC
import java.util.*

class MDCMiddleware : Middleware {
    override fun before(ctx: Context) {
        MDC.put(TRACE_ID.value, UUID.randomUUID().toString())
    }

    override fun after(ctx: Context) {
        ctx.header("X-trace", MDC.get(TRACE_ID.value))
        ctx.header("X-serverversion", config().getString("serverVersion"))
        MDC.remove(TRACE_ID.value)
    }
}