package printscript.authorization_service.server

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.aot.generate.Generated
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.UUID

@Component
@Generated
@Order(1)
class CorrelationIdFilter : Filter {
    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (request is HttpServletRequest && response is HttpServletResponse) {
            var correlationId = request.getHeader(CorrelationIdInterceptor.CORRELATION_ID_HEADER)
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString()
            }

            MDC.put(CorrelationIdInterceptor.CORRELATION_ID_KEY, correlationId)

            response.setHeader(CorrelationIdInterceptor.CORRELATION_ID_HEADER, correlationId)

            try {
                chain.doFilter(request, response)
            } finally {
                MDC.remove(CorrelationIdInterceptor.CORRELATION_ID_KEY)
            }
        } else {
            chain.doFilter(request, response)
        }
    }
}

