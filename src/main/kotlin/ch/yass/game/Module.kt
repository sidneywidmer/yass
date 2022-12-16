package ch.yass.game

import org.kodein.di.DI
import org.kodein.di.bindSingleton

object Game {
    val module = DI.Module("Game module") {
        bindSingleton { GameController() }
    }
}