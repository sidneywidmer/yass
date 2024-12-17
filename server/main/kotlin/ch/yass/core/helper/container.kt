package ch.yass.core.helper

import ch.yass.Yass
import ch.yass.core.CentrifugoClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.config.Config
import org.kodein.di.direct
import org.kodein.di.instance
import org.slf4j.Logger

/**
 * Global helper for quick access to the logging instance.
 */
fun logger(): Logger = Yass.container.direct.instance()

fun config(): Config = Yass.container.direct.instance()

fun jackson(): ObjectMapper = Yass.container.direct.instance<ObjectMapper>()