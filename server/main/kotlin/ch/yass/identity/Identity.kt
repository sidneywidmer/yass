package ch.yass.identity

import ch.yass.core.TraceInterceptor
import com.typesafe.config.Config
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import sh.ory.ApiClient
import sh.ory.api.FrontendApi

object Identity {
    val module = DI.Module("Auth module") {
        bindSingleton { AuthController(instance(), instance(), instance(), instance()) }
        bindSingleton { AuthMiddleware(instance(), instance()) }
        bindSingleton { ImpersonateMiddleware(instance()) }
        bindSingleton { createOryClient(instance(), instance()) }
    }

    private fun createOryClient(config: Config, traceInterceptor: TraceInterceptor): OryClient {
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(traceInterceptor)
            .build()

        val defaultClient = ApiClient(client).apply {
            basePath = config.getString("ory.basePath")
        }

        return OryClient(FrontendApi(defaultClient))
    }
}