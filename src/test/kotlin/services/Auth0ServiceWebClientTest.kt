package authorization.services

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

class Auth0ServiceWebClientTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var auth0Service: Auth0Service
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        auth0Service =
            Auth0Service(
                domain = "test-domain.auth0.com",
                clientId = "test-client-id",
                clientSecret = "test-client-secret",
            )

        // Replace WebClient with one pointing to mock server
        val webClient =
            WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build()
        ReflectionTestUtils.setField(auth0Service, "webClient", webClient)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getAccessToken should fetch new token when expired`() {
        // Given
        val tokenResponse =
            mapOf(
                "access_token" to "new-access-token",
                "token_type" to "Bearer",
                "expires_in" to 3600,
            )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(tokenResponse))
                .addHeader("Content-Type", "application/json"),
        )

        ReflectionTestUtils.setField(auth0Service, "accessToken", null)
        ReflectionTestUtils.setField(auth0Service, "tokenExpiresAt", 0L)

        // When - call getUserByAuth0Id which will trigger getAccessToken
        val auth0Id = "auth0|123"
        val userResponse =
            mapOf(
                "user_id" to auth0Id,
                "email" to "test@example.com",
                "name" to "Test User",
            )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(userResponse))
                .addHeader("Content-Type", "application/json"),
        )

        val result = auth0Service.getUserByAuth0Id(auth0Id).block()

        // Then
        result?.user_id shouldBeEqualTo auth0Id
        result?.email shouldBeEqualTo "test@example.com"
    }

    @Test
    fun `getUserByAuth0Id should return user when found`() {
        // Given
        val auth0Id = "auth0|123"
        val tokenResponse =
            mapOf(
                "access_token" to "test-token",
                "token_type" to "Bearer",
                "expires_in" to 3600,
            )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(tokenResponse))
                .addHeader("Content-Type", "application/json"),
        )

        val userResponse =
            mapOf(
                "user_id" to auth0Id,
                "email" to "test@example.com",
                "name" to "Test User",
            )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(userResponse))
                .addHeader("Content-Type", "application/json"),
        )

        // When
        val result = auth0Service.getUserByAuth0Id(auth0Id).block()

        // Then
        result?.user_id shouldBeEqualTo auth0Id
        result?.email shouldBeEqualTo "test@example.com"
        result?.name shouldBeEqualTo "Test User"
    }

    @Test
    fun `getUserByEmail should return user when found`() {
        // Given
        val email = "test@example.com"
        val tokenResponse =
            mapOf(
                "access_token" to "test-token",
                "token_type" to "Bearer",
                "expires_in" to 3600,
            )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(tokenResponse))
                .addHeader("Content-Type", "application/json"),
        )

        val userResponse =
            listOf(
                mapOf(
                    "user_id" to "auth0|123",
                    "email" to email,
                    "name" to "Test User",
                ),
            )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(userResponse))
                .addHeader("Content-Type", "application/json"),
        )

        // When
        val result = auth0Service.getUserByEmail(email).block()

        // Then
        result?.user_id shouldBeEqualTo "auth0|123"
        result?.email shouldBeEqualTo email
    }

    @Test
    fun `getAllUsers should return list of users`() {
        // Given
        val tokenResponse =
            mapOf(
                "access_token" to "test-token",
                "token_type" to "Bearer",
                "expires_in" to 3600,
            )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(tokenResponse))
                .addHeader("Content-Type", "application/json"),
        )

        val usersResponse =
            listOf(
                mapOf(
                    "user_id" to "auth0|123",
                    "email" to "test1@example.com",
                    "name" to "Test User 1",
                ),
                mapOf(
                    "user_id" to "auth0|456",
                    "email" to "test2@example.com",
                    "name" to "Test User 2",
                ),
            )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(usersResponse))
                .addHeader("Content-Type", "application/json"),
        )

        // When
        val result = auth0Service.getAllUsers().block()

        // Then
        result?.size shouldBeEqualTo 2
        result?.get(0)?.user_id shouldBeEqualTo "auth0|123"
        result?.get(1)?.user_id shouldBeEqualTo "auth0|456"
    }

    @Test
    fun `getUserByAuth0Id should handle errors`() {
        // Given
        val auth0Id = "auth0|123"
        val tokenResponse =
            mapOf(
                "access_token" to "test-token",
                "token_type" to "Bearer",
                "expires_in" to 3600,
            )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(tokenResponse))
                .addHeader("Content-Type", "application/json"),
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("Not Found"),
        )

        // When & Then
        StepVerifier.create(auth0Service.getUserByAuth0Id(auth0Id))
            .expectError()
            .verify()
    }
}
