package printscript.authorization_service

import authorization.AuthorizationServiceApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [AuthorizationServiceApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.oauth2.resourceserver." +
            "OAuth2ResourceServerAutoConfiguration",
    ],
)
@ActiveProfiles("test")
class AuthorizationServiceApplicationTests {
    @Test
    fun contextLoads() {
        // Test that the application context loads successfully
    }
}
