package ch.yass.core

import ch.yass.core.helper.config
import ch.yass.core.helper.logger
import ch.yass.core.pubsub.PubSub
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.core.FuelManager
import com.typesafe.config.ConfigFactory
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import org.slf4j.LoggerFactory
import java.sql.DriverManager
import com.typesafe.config.Config as ConfigSettings

object Core {
    val module = DI.Module("Core module") {
        bindSingleton { ConfigFactory.load() }
        bindSingleton { createDSLContext(instance()) }
        bindSingleton { Bootstrap(instance()) }
        bindSingleton { MDCMiddleware() }
        bindSingleton { LoggerFactory.getLogger("Yass") }
        bindSingleton { createJsonMapper() }
        bindSingleton { createCentrifugoClient() }
        bindSingleton { PubSub(instance(), instance()) }
    }

    private fun createCentrifugoClient(): CentrifugoClient {
        val manager = FuelManager().apply {
            baseHeaders = mapOf("X-API-Key" to config().getString("centrifugo.apiKey"))
            basePath = config().getString("centrifugo.basePath")
        }
        return CentrifugoClient(manager)
    }

    private fun createJsonMapper(): ObjectMapper {
        val mapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            dateFormat = StdDateFormat().withColonInTimeZone(true)
        }

        return mapper
    }

    private fun createDSLContext(config: ConfigSettings): DSLContext {
        System.setProperty("org.jooq.no-tips", "true");
        System.setProperty("org.jooq.no-logo", "true");

        val conn = DriverManager.getConnection(
            config.getString("db.url"),
            config.getString("db.username"),
            config.getString("db.password"),
        )

        return DSL.using(conn, SQLDialect.POSTGRES)
    }
}

