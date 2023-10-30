package ch.yass.core.helper

import ch.yass.Yass
import com.typesafe.config.Config
import org.kodein.di.direct
import org.kodein.di.instance
import org.slf4j.Logger

/**
 * Global helper for quick access to the logging instance.
 */
fun logger(): Logger {
    return Yass.container.direct.instance()
}

fun config(): Config {
    return Yass.container.direct.instance()
}
