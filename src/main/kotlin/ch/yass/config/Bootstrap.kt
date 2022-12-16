package ch.yass.config

import ch.qos.logback.classic.LoggerContext
import ch.yass.config.contract.Controller
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.event.EventListener
import org.kodein.di.DI
import org.kodein.di.allInstances
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*
import com.typesafe.config.Config as ConfigSettings


class Bootstrap(private val config: ConfigSettings) {
    fun start(di: DI) {
        val app = Javalin.create { config ->
            config.showJavalinBanner = false
        }

        app.before {
            MDC.put("requestId", UUID.randomUUID().toString())
        }

        app.after { ctx ->
            ctx.header("X-Request-UUID", MDC.get("requestId"))
            MDC.remove("requestId")
        }

        app.routes {
            val controllers: List<Controller> by di.allInstances()
            controllers.forEach { path(it.path, it.endpoints) }
        }

        // We want a clean shutdown
        Runtime.getRuntime().addShutdownHook(Thread { app.stop() })

        app.events { event: EventListener ->
            event.serverStopping {
                // Ensure all logback appenders get flushed
                val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
                loggerContext.stop()
            }
        }

        // app.exception(Exception::class.java, HandlerError::handlerErrorException)

        app.start(config.getInt("server.port"))
    }
}