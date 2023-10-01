package ch.yass.core.helper

import arrow.core.raise.Raise
import arrow.core.raise.catch
import ch.yass.core.error.StringNoValidUUID
import java.util.*

context(Raise<StringNoValidUUID>)
fun String.toUUID(): UUID = catch({ UUID.fromString(this) }) { raise(StringNoValidUUID(this)) }

