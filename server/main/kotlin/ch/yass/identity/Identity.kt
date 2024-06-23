package ch.yass.identity

import com.typesafe.config.Config
import okhttp3.OkHttpClient
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import org.zalando.logbook.Logbook
import org.zalando.logbook.okhttp.LogbookInterceptor
import sh.ory.ApiClient
import sh.ory.api.FrontendApi

object Identity {
    val module = DI.Module("Auth module") {
        bindSingleton { AuthController(instance(), instance(), instance()) }
        bindSingleton { AuthMiddleware(instance(), instance()) }
        bindSingleton { ImpersonateMiddleware(instance()) }
        bindSingleton { createOryClient(instance(), instance()) }
    }

    private fun createOryClient(config: Config, logbook: Logbook): OryClient {
        val client = OkHttpClient.Builder()
            .addNetworkInterceptor(LogbookInterceptor(logbook))
            .build()

        val defaultClient = ApiClient(client).apply {
            basePath = config.getString("ory.basePath")
        }

        return OryClient(FrontendApi(defaultClient))
    }
}