package ch.yass

import ch.yass.admin.Admin
import ch.yass.identity.Identity
import ch.yass.core.Bootstrap
import ch.yass.core.Core
import ch.yass.game.Game
import org.kodein.di.DI
import org.kodein.di.instance


class Yass {
    companion object {
        val container = DI {
            import(Core.module)
            import(Admin.module)
            import(Identity.module)
            import(Game.module)
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val bootstrap: Bootstrap by container.instance()
            bootstrap.start(container)
        }
    }
}