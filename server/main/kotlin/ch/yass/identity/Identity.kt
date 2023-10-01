package ch.yass.identity

import com.typesafe.config.Config
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import sh.ory.Configuration
import sh.ory.api.FrontendApi

object Identity {
    val module = DI.Module("Auth module") {
        bindSingleton { AuthController(instance(), instance()) }
        bindSingleton { AuthMiddleware(instance(), instance()) }
        bindSingleton { createOryClient(instance()) }
    }

    private fun createOryClient(config: Config): OryClient {
        val defaultClient = Configuration.getDefaultApiClient()
        defaultClient.basePath = config.getString("ory.basePath")

        return OryClient(FrontendApi(defaultClient))
    }
}