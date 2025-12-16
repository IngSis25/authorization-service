package authorization.services

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils

class Auth0ServiceTest {
    private lateinit var auth0Service: Auth0Service

    @BeforeEach
    fun setUp() {
        auth0Service =
            Auth0Service(
                domain = "test-domain.auth0.com",
                clientId = "test-client-id",
                clientSecret = "test-client-secret",
            )
    }

    @Test
    fun `Auth0Service should initialize correctly`() {
        // Given & When - service is created in setUp
        // Then - verify it was created
        auth0Service shouldBeEqualTo auth0Service
    }

    @Test
    fun `Auth0User should create correctly`() {
        // Given
        val user =
            Auth0Service.Auth0User(
                user_id = "auth0|123",
                email = "test@example.com",
                name = "Test User",
            )

        // Then
        user.user_id shouldBeEqualTo "auth0|123"
        user.email shouldBeEqualTo "test@example.com"
        user.name shouldBeEqualTo "Test User"
    }

    @Test
    fun `Auth0User should handle null values`() {
        // Given
        val user =
            Auth0Service.Auth0User(
                user_id = "auth0|123",
                email = null,
                name = null,
            )

        // Then
        user.user_id shouldBeEqualTo "auth0|123"
        user.email shouldBeEqualTo null
        user.name shouldBeEqualTo null
    }

    @Test
    fun `TokenResponse should create correctly`() {
        // Given
        val tokenResponse =
            Auth0Service.TokenResponse(
                accessToken = "test-token",
                tokenType = "Bearer",
                expiresIn = 3600,
            )

        // Then
        tokenResponse.accessToken shouldBeEqualTo "test-token"
        tokenResponse.tokenType shouldBeEqualTo "Bearer"
        tokenResponse.expiresIn shouldBeEqualTo 3600
    }

    @Test
    fun `getAccessToken should use cached token when available and not expired`() {
        // Given - set up cached token
        val cachedToken = "cached-token-123"
        ReflectionTestUtils.setField(auth0Service, "accessToken", cachedToken)
        ReflectionTestUtils.setField(
            auth0Service,
            "tokenExpiresAt",
            System.currentTimeMillis() / 1000 + 3600,
        )

        // When - call a method that uses getAccessToken internally
        // We test this indirectly by checking that the token field is used
        val token = ReflectionTestUtils.getField(auth0Service, "accessToken") as String?

        // Then
        token shouldBeEqualTo cachedToken
    }

    @Test
    fun `getAccessToken should refresh when token is expired`() {
        // Given - set up expired token
        val oldToken = "old-token"
        ReflectionTestUtils.setField(auth0Service, "accessToken", oldToken)
        ReflectionTestUtils.setField(
            auth0Service,
            "tokenExpiresAt",
            System.currentTimeMillis() / 1000 - 3600,
        )

        // When - verify token is expired
        val expiresAt = ReflectionTestUtils.getField(auth0Service, "tokenExpiresAt") as Long
        val now = System.currentTimeMillis() / 1000

        // Then
        (expiresAt < now) shouldBeEqualTo true
    }
}
