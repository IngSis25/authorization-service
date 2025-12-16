package authorization.app

import authorization.server.CorrelationIdInterceptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class AppConfig {
    @Autowired(required = false)
    private var correlationIdInterceptor: CorrelationIdInterceptor? = null

    @Bean
    fun restTemplate(): RestTemplate {
        val restTemplate = RestTemplate()
        correlationIdInterceptor?.let {
            restTemplate.interceptors.add(it) // Adding the interceptor if available
        }
        return restTemplate
    }
}
