package ch.yass.integration

import ch.yass.Yass
import ch.yass.db.tables.references.*
import ch.yass.testcontainer.PostgresContainer
import ch.yass.wiremock.CentrifugoServer
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.kodein.di.direct
import org.kodein.di.instance


open class Integration {
    protected val container = Yass.container
    protected val centrifugo = CentrifugoServer.instance

    companion object {
        @JvmStatic
        @BeforeAll
        fun init() {
            val db = PostgresContainer.instance

            System.setProperty("db.url", db.jdbcUrl)
            System.setProperty("db.username", db.username)
            System.setProperty("db.password", db.password)
            System.setProperty("db.migrate", "false") // Migrations are handled by postgres container
            System.setProperty("environment", "test")
        }
    }

    @BeforeEach
    fun before() {
        cleanDB()
    }

    private fun cleanDB() {
        val db: DSLContext = Yass.container.direct.instance()
        val tables = listOf(GAME, PLAYER, TRICK, SEAT, HAND)
        tables.forEach {
            db.execute("TRUNCATE TABLE ${it.name} RESTART IDENTITY CASCADE")
        }
    }
}
