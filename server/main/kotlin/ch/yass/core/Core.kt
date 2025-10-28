package ch.yass.core

import ch.yass.Yass
import ch.yass.core.helper.config
import ch.yass.core.middleware.MDCMiddleware
import ch.yass.core.middleware.RequestLoggerMiddleware
import ch.yass.core.pubsub.PubSub
import ch.yass.game.dto.db.InternalPlayer
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import okhttp3.OkHttpClient
import org.jobrunr.configuration.JobRunr
import org.jobrunr.scheduling.JobScheduler
import org.jobrunr.server.BackgroundJobServerConfiguration
import org.jobrunr.server.JobActivator
import org.jobrunr.storage.InMemoryStorageProvider
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.type.erased
import org.slf4j.LoggerFactory
import com.typesafe.config.Config as ConfigSettings


object Core {
    val module = DI.Module("Core module") {
        bindSingleton { ConfigFactory.load() }
        bindSingleton { createDSLContext(instance()) }
        bindSingleton { Bootstrap(instance()) }
        bindSingleton { MDCMiddleware() }
        bindSingleton { RequestLoggerMiddleware() }
        bindSingleton { TraceInterceptor() }
        bindSingleton { LoggerFactory.getLogger("Yass") }
        bindSingleton { createJsonMapper() }
        bindSingleton { createCentrifugoClient(instance()) }
        bindSingleton { PubSub(instance(), instance()) }
        bindSingleton { createJobRunr() }
    }

    private fun createJobRunr(): JobScheduler {
        val config = BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration()
            .andPollIntervalInSeconds(10)

        // JobRunr needs to be able to resolve dependencies from our kodein container
        val jobActivator: JobActivator = object : JobActivator {
            override fun <T : Any> activateJob(jobClass: Class<T>): T = Yass.container.direct.Instance(erased(jobClass))
        }

        return JobRunr.configure()
            .useStorageProvider(InMemoryStorageProvider())
            .useJobActivator(jobActivator)
            .useBackgroundJobServer(config, true)
            .initialize()
            .jobScheduler
    }

    private fun createCentrifugoClient(traceInterceptor: TraceInterceptor): CentrifugoClient {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestWithApiKey = chain.request().newBuilder()
                    .header("X-API-Key", config().getString("centrifugo.apiKey"))
                    .build()

                chain.proceed(requestWithApiKey)
            }
            .addInterceptor(traceInterceptor)
            .build()

        return CentrifugoClient(client)
    }

    private fun createJsonMapper(): ObjectMapper {
        val mapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            dateFormat = StdDateFormat().withColonInTimeZone(true)

            // Make sure to never serialize
            configOverride(InternalPlayer::class.java).setIsIgnoredType(true)
        }

        return mapper
    }

    private fun createDSLContext(config: ConfigSettings): DSLContext {
        System.setProperty("org.jooq.no-tips", "true")
        System.setProperty("org.jooq.no-logo", "true")

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.getString("db.url")
            username = config.getString("db.username")
            password = config.getString("db.password")
            maximumPoolSize = 10
            driverClassName = "org.postgresql.Driver"
        }
        val conn = HikariDataSource(hikariConfig)

        return DSL.using(conn, SQLDialect.POSTGRES)
    }
}

