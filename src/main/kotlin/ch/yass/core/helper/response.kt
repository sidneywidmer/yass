package ch.yass.core.helper

import ch.yass.core.error.DomainError
import ch.yass.core.error.ValiktorError
import io.javalin.http.Context
import org.valiktor.i18n.mapToMessage
import java.util.*

data class ErrorResponse(val message: String = "", val payload: Any? = null)
data class Path(val path: String, val violations: List<Violation>)
data class Violation(val code: String, val message: String = "")

/**
 * Helper to json encode given data with jackson and add the result
 * to the context.
 */
fun successResponse(ctx: Context, data: Any): Context {
    return ctx.status(200).json(data)
}

fun errorResponse(ctx: Context, error: DomainError): Context {
    return when (error) {
        is ValiktorError -> ctx.status(422).json(groupValiktorViolations(error))
        else -> {
            logger().error("DomainError `${error.javaClass.name}` encountered: $error")
            ctx.status(500).json(ErrorResponse("ouch"))
        }
    }
}

/**
 * {
 *     "code": "constraint.violations",
 *     "message": "invalid data",
 *     "payload": [
 *         {"path" : "firstname", "violations": ["code": "NotEmpty", "message": "firstname cannot be empty"]}
 *     ]
 * }
 */
private fun groupValiktorViolations(error: ValiktorError): ErrorResponse {
    val payload = error.violations
        .mapToMessage("messages", Locale.ENGLISH)
        .groupBy { it.property }
        .map { Path(it.key, it.value.map { violation -> Violation(violation.constraint.name, violation.message) }) }

    return ErrorResponse("constraint violations found", payload)
}
