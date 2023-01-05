package ch.yass.core.helper

import ch.yass.Yass
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.JSON
import org.kodein.di.direct
import org.kodein.di.instance

fun toJson(value: Any?): String {
    val mapper = Yass.container.direct.instance<ObjectMapper>()
    return mapper.writeValueAsString(value)
}

fun toDbJson(value: Any?): JSON? {
    return JSON.jsonOrNull(toJson(value))
}

inline fun <reified T> listFromDbJson(json: JSON?): List<T> {
    val mapper = Yass.container.direct.instance<ObjectMapper>()
    return mapper.readerForListOf(T::class.java).readValue(json.toString())
}