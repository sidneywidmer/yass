package ch.yass.core

import ch.yass.Yass
import ch.yass.core.helper.config
import ch.yass.core.pubsub.PubSub
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.ConfigFactory
import okhttp3.OkHttpClient
import org.jobrunr.configuration.JobRunr
import org.jobrunr.configuration.JobRunrConfiguration
import org.jobrunr.scheduling.JobScheduler
import org.jobrunr.server.BackgroundJobServerConfiguration
import org.jobrunr.server.JobActivator
import org.jobrunr.storage.InMemoryStorageProvider
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.kodein.di.*
import org.kodein.type.erased
import org.kodein.type.typeToken
import org.slf4j.LoggerFactory
import org.zalando.logbook.Logbook
import org.zalando.logbook.core.*
import org.zalando.logbook.core.HeaderFilters.replaceCookies
import org.zalando.logbook.core.HeaderFilters.replaceHeaders
import org.zalando.logbook.json.JsonHttpLogFormatter
import org.zalando.logbook.okhttp.LogbookInterceptor
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
        bindSingleton { createLogbook() }
        bindSingleton { createCentrifugoClient(instance()) }
        bindSingleton { PubSub(instance(), instance()) }
        bindSingleton { createJobRunr() }
    }

    private fun createJobRunr(): JobScheduler {
        val config = BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration()
            .andPollIntervalInSeconds(10)

        // JobRunr needs to be able to resolve dependencies from our kodein container
        val jobActivator: JobActivator = object : JobActivator {
            override fun <T : Any> activateJob(jobClass: Class<T>): T {
                return Yass.container.direct.Instance(erased(jobClass))
            }
        }

        return JobRunr.configure()
            .useStorageProvider(InMemoryStorageProvider())
            .useJobActivator(jobActivator)
            .useBackgroundJobServer(config, true)
            .initialize()
            .jobScheduler
    }

    private fun createLogbook(): Logbook {
        val sink = DefaultSink(JsonHttpLogFormatter(), DefaultHttpLogWriter())

        return Logbook.builder()
            .headerFilter(replaceCookies("ory_kratos_session"::equals, "logbook-replaced"))
            .headerFilter(replaceHeaders("x-api-key"::equals, "logbook-replaced"))
            .sink(sink)
            .build()
    }

    private fun createCentrifugoClient(logbook: Logbook): CentrifugoClient {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestWithApiKey = chain.request().newBuilder()
                    .header("X-API-Key", config().getString("centrifugo.apiKey"))
                    .build()

                chain.proceed(requestWithApiKey)
            }
            .addNetworkInterceptor(LogbookInterceptor(logbook))
            .build()

        return CentrifugoClient(client)
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
        System.setProperty("org.jooq.no-tips", "true")
        System.setProperty("org.jooq.no-logo", "true")

        val conn = DriverManager.getConnection(
            config.getString("db.url"),
            config.getString("db.username"),
            config.getString("db.password"),
        )

        return DSL.using(conn, SQLDialect.POSTGRES)
    }
}

