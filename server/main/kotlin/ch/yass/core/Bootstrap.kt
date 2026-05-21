package ch.yass.core

import ch.qos.logback.classic.LoggerContext
import ch.yass.core.contract.Controller
import ch.yass.core.contract.Middleware
import ch.yass.core.error.DomainException
import ch.yass.core.error.domainExceptionHandler
import ch.yass.core.error.globalExceptionHandler
import ch.yass.core.middleware.MDCMiddleware
import ch.yass.core.middleware.RequestLoggerMiddleware
import ch.yass.identity.AuthMiddleware
import ch.yass.identity.ImpersonateMiddleware
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.json.JavalinJackson
import org.flywaydb.core.Flyway
import org.kodein.di.DI
import org.kodein.di.allInstances
import org.kodein.di.direct
import org.kodein.di.instance
import org.slf4j.LoggerFactory
import com.typesafe.config.Config as ConfigSettings


class Bootstrap(private val config: ConfigSettings) {
    fun start(di: DI) {
        setupFlyway()

        // Register all middlewares here. We're doing this manually to ensure
        // the correct order (opposed to just also use di.allInstances()).
        val middlewares: List<Middleware> = listOf(
            di.direct.instance<MDCMiddleware>(),
            di.direct.instance<RequestLoggerMiddleware>(),
            di.direct.instance<AuthMiddleware>(),
            di.direct.instance<ImpersonateMiddleware>(),
        )

        val app = Javalin.create { javalinConfig ->
            javalinConfig.startup.showJavalinBanner = false
            javalinConfig.jsonMapper(JavalinJackson(di.direct.instance()))
            javalinConfig.concurrency.useVirtualThreads = true
            javalinConfig.routes.apiBuilder {
                val controllers: List<Controller> by di.allInstances()
                controllers.forEach { path(it.path, it.endpoints) }
            }
            javalinConfig.bundledPlugins.enableCors { cors ->
                cors.addRule {
                    it.allowHost(config.getString("server.cors"))
                    it.allowCredentials = true
                }
            }
            javalinConfig.routes.exception(DomainException::class.java) { exception, ctx ->
                domainExceptionHandler(ctx, exception)
            }
            javalinConfig.routes.exception(Exception::class.java) { exception, ctx ->
                globalExceptionHandler(exception, ctx)
            }
            javalinConfig.events.serverStopping {
                // Ensure all logback appenders get flushed
                val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
                loggerContext.stop()
            }
            javalinConfig.routes.beforeMatched { ctx ->
                middlewares.forEach { it.before(ctx) }
            }
            javalinConfig.routes.afterMatched { ctx ->
                middlewares.reversed().forEach { it.after(ctx) }
            }
        }

        // We want a clean shutdown
        Runtime.getRuntime().addShutdownHook(Thread { app.stop() })

        app.start("0.0.0.0", config.getInt("server.port"))
    }

    private fun setupFlyway() {
        val flyway = Flyway.configure()
            .loggers("slf4j")
            .dataSource(config.getString("db.url"), config.getString("db.username"), config.getString("db.password"))
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .validateMigrationNaming(true)
            .load()

        if (config.getBoolean("db.migrate")) {
            flyway.migrate()
        }
    }
}
