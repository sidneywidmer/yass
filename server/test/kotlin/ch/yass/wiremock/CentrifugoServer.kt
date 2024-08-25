package ch.yass.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options


object CentrifugoServer {
    val instance by lazy { startServer() }

    private fun startServer(): WireMockServer {
        val server = WireMockServer(options().port(8000));
        server.start()
        return server
    }
}
