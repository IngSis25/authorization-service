package authorization.security

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt

class AudienceValidatorTest {
    private lateinit var audienceValidator: AudienceValidator
    private val audience = "test-audience"

    @BeforeEach
    fun setUp() {
        audienceValidator = AudienceValidator(audience)
    }

    @Test
    fun `validate should return success when audience matches`() {
        // Given
        val jwt =
            Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("aud", listOf(audience))
                .build()

        // When
        val result = audienceValidator.validate(jwt)

        // Then
        result.hasErrors() shouldBeEqualTo false
    }

    @Test
    fun `validate should return failure when audience does not match`() {
        // Given
        val jwt =
            Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("aud", listOf("other-audience"))
                .build()

        // When
        val result = audienceValidator.validate(jwt)

        // Then
        result.hasErrors() shouldBeEqualTo true
        result.errors.isNotEmpty() shouldBeEqualTo true
        result.errors.first().errorCode shouldBeEqualTo "invalid_token"
    }

    @Test
    fun `validate should return failure when audience is empty`() {
        // Given
        val jwt =
            Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("aud", emptyList<String>())
                .build()

        // When
        val result = audienceValidator.validate(jwt)

        // Then
        result.hasErrors() shouldBeEqualTo true
    }
}
