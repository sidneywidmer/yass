package ch.yass.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options


object CentrifugoServer {
    val instance by lazy { startServer() }

    private fun startServer(): WireMockServer {
        val server = WireMockServer(options().port(8000))
        server.start()

        // We currently only do one request to centrifugo, and that is publishing to channels. If this gets more
        // complicated it needs improvements but for now this is enough
        WireMock.configureFor("127.0.0.1", server.port());
        stubFor(post("/api/publish").willReturn(ok()));

        return server
    }
}
