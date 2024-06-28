package ch.yass

import ch.yass.admin.Admin
import ch.yass.core.Bootstrap
import ch.yass.core.Core
import ch.yass.game.Game
import ch.yass.identity.Identity
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton


class Yass {
    companion object {
        var container = DI {
            import(Core.module)
            import(Admin.module)
            import(Identity.module)
            import(Game.module)
        }

        /**
         * Allows us to overwrite any singleton in our container. This should mainly be used for testing
         * purposes, for example like this:
         *
         * val mock = mockk<CentrifugoClient>()
         * Yass.containerOverride(mock)
         */
        inline fun <reified T : Any> containerOverride(replacement: T) {
            container = DI {
                extend(container, allowOverride = true)
                import(DI.Module("override") {
                    bind<T>(overrides = true) with singleton { replacement }
                }, allowOverride = true)
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val bootstrap: Bootstrap by container.instance()
            bootstrap.start(container)
        }
    }
}