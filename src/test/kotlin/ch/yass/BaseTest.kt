package ch.yass

import ch.yass.core.Core
import ch.yass.db.tables.references.*
import ch.yass.game.Game
import ch.yass.identity.Identity
import org.jooq.DSLContext
import org.junit.Before
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance


open class BaseTest {
    companion object {
        val container = DI {
            import(Core.module)
            import(Identity.module)
            import(Game.module)
        }
    }

    @Before
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