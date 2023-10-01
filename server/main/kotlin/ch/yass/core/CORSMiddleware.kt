package ch.yass.core

import ch.yass.core.contract.Middleware
import io.javalin.http.Context
import org.slf4j.MDC
import java.util.*

class CORSMiddleware : Middleware {
    override fun before(ctx: Context) {}

    override fun after(ctx: Context) {
        ctx.header("Access-Control-Allow-Origin", "http://127.0.0.1:5173")  // webfrontend
        ctx.header("Access-Control-Allow-Credentials", "true")
    }
}