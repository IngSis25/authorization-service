package printscript.authorization_service.server

import org.slf4j.MDC
import org.springframework.aot.generate.Generated
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Generated
class CorrelationIdInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val correlationId = MDC.get(CORRELATION_ID_KEY) ?: UUID.randomUUID().toString()
        request.headers.add(CORRELATION_ID_HEADER, correlationId)
        return execution.execute(request, body)
    }

    companion object {
        const val CORRELATION_ID_KEY: String = "correlation-id"
        const val CORRELATION_ID_HEADER: String = "X-Correlation-Id"
    }
}

