package ch.yass

import ch.yass.auth.Auth
import ch.yass.config.Bootstrap
import ch.yass.config.Config
import ch.yass.game.Game
import org.kodein.di.DI
import org.kodein.di.instance


fun main() {
    val di = DI {
        import(Config.module)
        import(Auth.module)
        import(Game.module)
    }

    val bootstrap: Bootstrap by di.instance()
    bootstrap.start(di)
}

