package ch.yass

import ch.yass.db.tables.references.*
import org.junit.jupiter.api.*
import org.jooq.DSLContext
import org.kodein.di.direct
import org.kodein.di.instance


open class BaseTest {
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