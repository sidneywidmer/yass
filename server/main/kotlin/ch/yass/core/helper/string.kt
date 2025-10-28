package ch.yass.core.helper

import arrow.core.raise.Raise
import arrow.core.raise.catch
import ch.yass.core.error.StringNoValidUUID
import java.util.*
import kotlin.random.Random

context(Raise<StringNoValidUUID>)
fun String.toUUID(): UUID = catch({ UUID.fromString(this) }) { raise(StringNoValidUUID(this)) }

fun correlationId(): String {
    return Random.nextInt().toLong().and(0xFFFFFFFFL).toString(16).padStart(8, '0')
}


