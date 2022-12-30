package ch.yass.core.helper

import ch.yass.Yass
import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.http.Context
import org.kodein.di.direct
import org.kodein.di.instance

/**
 * Helper to json encode given data with jackson and add the result
 * to the context.
 */
fun response(ctx: Context, data: Any): Context {
    val mapper = Yass.container.direct.instance<ObjectMapper>()

    return ctx.status(200).json(mapper.writeValueAsString(data))
}