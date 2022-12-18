package ch.yass

import ch.yass.identity.Identity
import ch.yass.core.Bootstrap
import ch.yass.core.Core
import ch.yass.game.Game
import org.kodein.di.DI
import org.kodein.di.instance


fun main() {
    val di = DI {
        import(Core.module)
        import(Identity.module)
        import(Game.module)
    }

    val bootstrap: Bootstrap by di.instance()
    bootstrap.start(di)
}

