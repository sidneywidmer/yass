package ch.yass.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.ConfigFactory
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import com.typesafe.config.Config as ConfigSettings
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.DriverManager

object Core {
    val module = DI.Module("Core module") {
        bindSingleton { ConfigFactory.load() }
        bindSingleton { createDSLContext(instance()) }
        bindSingleton { Bootstrap(instance()) }
        bindSingleton { MDCMiddleware() }
        bindSingleton { LoggerFactory.getLogger("Yass") }
        bindSingleton { createJsonMapper() }
    }

    private fun createJsonMapper(): ObjectMapper {
        val mapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            dateFormat = StdDateFormat().withColonInTimeZone(true)
        }

        return mapper
    }

    private fun createDSLContext(config: ConfigSettings): DSLContext {
        val conn = DriverManager.getConnection(
            config.getString("db.url"),
            config.getString("db.username"),
            config.getString("db.password"),
        )

        System.setProperty("org.jooq.no-tips", "true");
        System.setProperty("org.jooq.no-logo", "true");

        return DSL.using(conn, SQLDialect.POSTGRES)
    }
}

