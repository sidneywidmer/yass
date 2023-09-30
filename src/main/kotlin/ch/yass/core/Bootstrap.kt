package ch.yass.core

import ch.qos.logback.classic.LoggerContext
import ch.yass.identity.AuthMiddleware
import ch.yass.core.contract.Controller
import ch.yass.core.contract.Middleware
import ch.yass.core.error.DomainException
import ch.yass.core.error.domainExceptionHandler
import ch.yass.core.error.globalExceptionHandler
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.event.EventListener
import io.javalin.json.JavalinJackson
import org.kodein.di.*
import org.slf4j.LoggerFactory
import com.typesafe.config.Config as ConfigSettings
import org.flywaydb.core.Flyway


class Bootstrap(private val config: ConfigSettings) {
    fun start(di: DI) {
        if (config.getBoolean("db.migrate")) {
            migrateDB()
        }

        val app = Javalin.create { config ->
            config.showJavalinBanner = false
            config.jsonMapper(JavalinJackson(di.direct.instance()))
        }

        // Register all middlewares here. We're doing this manually to ensure
        // the correct order (opposed to just also use di.allInstances()).
        val middlewares: List<Middleware> = listOf(
            di.direct.instance<MDCMiddleware>(),
            di.direct.instance<AuthMiddleware>(),
        )
        registerMiddlewares(app, middlewares)

        val controllers: List<Controller> by di.allInstances()
        registerRoutes(app, controllers)

        registerEvents(app)

        // We want a clean shutdown
        Runtime.getRuntime().addShutdownHook(Thread { app.stop() })

        app.exception(DomainException::class.java) { exception, ctx -> domainExceptionHandler(ctx, exception) }
        app.exception(Exception::class.java) { exception, ctx -> globalExceptionHandler(exception, ctx) }
    }

    private fun migrateDB() {
        Flyway.configure()
            .dataSource(config.getString("db.url"), config.getString("db.username"), config.getString("db.password"))
            .locations("filesystem:src/main/resources/db/migration")
            .baselineOnMigrate(true)
            .load()
            .migrate()
    }

    private fun registerEvents(app: Javalin) {
        app.events { event: EventListener ->
            event.serverStopping {
                // Ensure all logback appenders get flushed
                val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
                loggerContext.stop()
            }
        }
    }

    private fun registerRoutes(app: Javalin, controllers: List<Controller>) {
        app.routes {
            controllers.forEach { path(it.path, it.endpoints) }
        }
    }

    private fun registerMiddlewares(app: Javalin, middlewares: List<Middleware>) {
        app.before { ctx ->
            middlewares.forEach { it.before(ctx) }
        }

        app.after { ctx ->
            middlewares.forEach { it.after(ctx) }
        }
    }
}