package ch.yass.core.helper

import arrow.core.raise.Raise
import ch.yass.core.error.StringNoValidUUID
import java.util.*
import kotlin.random.Random

context(r: Raise<StringNoValidUUID>)
fun String.toUUID(): UUID = try { UUID.fromString(this) } catch (_: IllegalArgumentException) { r.raise(StringNoValidUUID(this)) }

fun correlationId(): String {
    return Random.nextInt().toLong().and(0xFFFFFFFFL).toString(16).padStart(8, '0')
}


