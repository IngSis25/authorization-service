package authorization.auth0

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

@ExtendWith(MockitoExtension::class)
class Auth0ManagementServiceTest {
    @Mock
    private lateinit var restTemplate: RestTemplate

    private lateinit var auth0ManagementService: Auth0ManagementService

    @BeforeEach
    fun setUp() {
        auth0ManagementService =
            Auth0ManagementService(
                restTemplate = restTemplate,
                domain = "test-domain.auth0.com",
                clientId = "test-client-id",
                clientSecret = "test-client-secret",
                audience = "https://test-domain.auth0.com/api/v2/",
            )
    }

    @Test
    fun `getManagementToken should return access token`() {
        // Given
        val tokenResponse =
            """
            {
                "access_token": "test-access-token",
                "token_type": "Bearer",
                "expires_in": 3600
            }
            """.trimIndent()

        val response = ResponseEntity(tokenResponse, HttpStatus.OK)
        whenever(
            restTemplate.postForEntity(
                any<String>(),
                any<HttpEntity<*>>(),
                any<Class<*>>(),
            ),
        ).thenReturn(response)

        // When
        val result = auth0ManagementService.getManagementToken()

        // Then
        result shouldBeEqualTo "test-access-token"
    }

    @Test
    fun `getManagementToken should throw exception when request fails`() {
        // Given
        val response = ResponseEntity<String>("Error", HttpStatus.INTERNAL_SERVER_ERROR)
        whenever(
            restTemplate.postForEntity(
                any<String>(),
                any<HttpEntity<*>>(),
                any<Class<*>>(),
            ),
        ).thenReturn(response)

        // When & Then
        try {
            auth0ManagementService.getManagementToken()
            assert(false) { "Should have thrown exception" }
        } catch (e: IllegalStateException) {
            e.message?.contains("Error getting Auth0 management token") shouldBeEqualTo true
        }
    }

    @Test
    fun `searchUsersByEmailFragment should return empty list when query is too short`() {
        // Given
        val query = "ab"

        // When
        val result = auth0ManagementService.searchUsersByEmailFragment(query)

        // Then
        result.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `searchUsersByEmailFragment should return empty list when query is empty`() {
        // Given
        val query = ""

        // When
        val result = auth0ManagementService.searchUsersByEmailFragment(query)

        // Then
        result.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `searchUsersByEmailFragment should return users when found`() {
        // Given
        val query = "test"
        val tokenResponse =
            """
            {
                "access_token": "test-access-token",
                "token_type": "Bearer",
                "expires_in": 3600
            }
            """.trimIndent()

        val tokenHttpResponse = ResponseEntity(tokenResponse, HttpStatus.OK)
        whenever(
            restTemplate.postForEntity(
                any<String>(),
                any<HttpEntity<*>>(),
                any<Class<*>>(),
            ),
        ).thenReturn(tokenHttpResponse)

        val users =
            listOf(
                Auth0User(
                    user_id = "auth0|123",
                    email = "test@example.com",
                ),
            )

        val usersResponse: ResponseEntity<List<Auth0User>> = ResponseEntity(users, HttpStatus.OK)
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<List<Auth0User>>>(),
            ),
        ).thenReturn(usersResponse)

        // When
        val result = auth0ManagementService.searchUsersByEmailFragment(query)

        // Then
        result.size shouldBeEqualTo 1
        result[0].user_id shouldBeEqualTo "auth0|123"
        result[0].email shouldBeEqualTo "test@example.com"
    }

    @Test
    fun `searchUsersByEmailFragment should return empty list when response body is null`() {
        // Given
        val query = "test"
        val tokenResponse =
            """
            {
                "access_token": "test-access-token",
                "token_type": "Bearer",
                "expires_in": 3600
            }
            """.trimIndent()

        val tokenHttpResponse = ResponseEntity(tokenResponse, HttpStatus.OK)
        whenever(
            restTemplate.postForEntity(
                any<String>(),
                any<HttpEntity<*>>(),
                any<Class<*>>(),
            ),
        ).thenReturn(tokenHttpResponse)

        val usersResponse: ResponseEntity<List<Auth0User>> = ResponseEntity(null, HttpStatus.OK)
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<List<Auth0User>>>(),
            ),
        ).thenReturn(usersResponse)

        // When
        val result = auth0ManagementService.searchUsersByEmailFragment(query)

        // Then
        result.isEmpty() shouldBeEqualTo true
    }
}
