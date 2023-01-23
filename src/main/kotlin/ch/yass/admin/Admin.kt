package ch.yass.admin

import org.kodein.di.DI
import org.kodein.di.bindSingleton

object Admin {
    val module = DI.Module("Admin module") {
        bindSingleton { AdminController() }
    }
}