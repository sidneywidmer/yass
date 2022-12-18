package ch.yass.core

import ch.yass.core.contract.Middleware
import io.javalin.http.Context
import org.slf4j.MDC
import java.util.*

/**
 * Before every request we're adding a unique request id to the MD context. This
 * `requestId` is also referenced in the logback logtail configuration. The help
 * debugging problems this id is also set as header on every response.
 */
class MDCMiddleware : Middleware {
    override fun before(ctx: Context) {
        MDC.put("requestId", UUID.randomUUID().toString())
    }

    override fun after(ctx: Context) {
        ctx.header("X-Request-UUID", MDC.get("requestId"))
        MDC.remove("requestId")
    }
}