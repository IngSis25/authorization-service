package authorization.app

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebMvc
@Profile("!test")
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedOrigins(
                "http://localhost:5173",
                "http://localhost:8001",
            )
            .allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH", "OPTIONS")
            .allowedHeaders("*")
    }
}
