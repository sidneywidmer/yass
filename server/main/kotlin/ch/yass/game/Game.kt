package ch.yass.game

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.kodein.di.DI
import org.kodein.di.bindEagerSingleton
import org.kodein.di.bindSingleton
import org.kodein.di.instance

object Game {
    val module = DI.Module("Game module") {
        bindSingleton { createDispatcher() }
        bindSingleton { GameController(instance(), instance()) }
        bindSingleton { PlayerService(instance()) }
        bindSingleton { GameRepository(instance()) }
        bindSingleton { GameService(instance(), instance(), instance()) }
        bindEagerSingleton { JobsService(instance(), instance(), instance()) }
    }

    private fun createDispatcher(): CoroutineDispatcher = Dispatchers.Default
}