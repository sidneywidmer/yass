package ch.yass.core.contract

import io.javalin.http.Context

interface Middleware {
    fun before(ctx: Context)

    fun after(ctx: Context)
}
