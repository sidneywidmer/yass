package ch.yass.config

import com.typesafe.config.ConfigFactory
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import com.typesafe.config.Config as ConfigSettings
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import java.sql.DriverManager

object Config {
    val module = DI.Module("Config module") {
        bindSingleton { ConfigFactory.load() }
        bindSingleton { createDSLContext(instance()) }
        bindSingleton { Bootstrap(instance()) }
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

