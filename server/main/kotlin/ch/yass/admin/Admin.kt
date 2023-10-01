package ch.yass.admin

import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance

object Admin {
    val module = DI.Module("Admin module") {
        bindSingleton { AdminController(instance()) }
    }
}