package printscript.authorization_service.app

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import printscript.authorization_service.server.CorrelationIdInterceptor

@Configuration
class AppConfig(private val correlationIdInterceptor: CorrelationIdInterceptor) {

    @Bean
    fun restTemplate(): RestTemplate {
        val restTemplate = RestTemplate()
        restTemplate.interceptors.add(correlationIdInterceptor) // Adding the interceptor
        return restTemplate
    }
}

