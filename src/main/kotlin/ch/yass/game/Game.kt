package ch.yass.game

import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

object Game {
    val module = DI.Module("Game module") {
        bindSingleton { GameController(instance()) }
        bindSingleton { PlayerService() }
    }
}