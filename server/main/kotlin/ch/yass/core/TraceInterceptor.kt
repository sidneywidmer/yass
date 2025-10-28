package ch.yass.core

import ch.yass.core.contract.MDCAttributes.*
import ch.yass.core.helper.correlationId
import ch.yass.core.helper.logger
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.slf4j.MDC

class TraceInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val correlationId = correlationId()
        val requestWithTraceId = chain.request().newBuilder()
            .header("X-Trace-ID", MDC.get(TRACE_ID.value))
            .build()

        logger().info(buildRequestLog(correlationId, requestWithTraceId))

        val response = try {
            chain.proceed(requestWithTraceId)
        } catch (e: Exception) {
            logger().error("Request[$correlationId] failed: ${requestWithTraceId.method} ${requestWithTraceId.url}", e)
            throw e
        }

        logger().info(buildResponseLog(correlationId, response))

        return response
    }

    private fun buildRequestLog(correlationId: String, request: Request): String {
        val buffer = Buffer()
        request.body?.writeTo(buffer)

        return "Request[$correlationId] ${request.method} ${request.url} | Body: ${buffer.readUtf8()}"
    }

    private fun buildResponseLog(correlationId: String, response: Response): String {
        val body = response.peekBody(10 * 1024L).string().trim()
        return "Response[$correlationId] ${response.code} ${response.message} | Body: $body"
    }

}
