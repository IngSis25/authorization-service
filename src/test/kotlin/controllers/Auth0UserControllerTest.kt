package authorization.controllers

import authorization.auth0.Auth0ManagementService
import authorization.auth0.Auth0User
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class Auth0UserControllerTest {
    @Mock
    private lateinit var auth0ManagementService: Auth0ManagementService

    @InjectMocks
    private lateinit var auth0UserController: Auth0UserController

    private lateinit var auth0Users: List<Auth0User>

    @BeforeEach
    fun setUp() {
        auth0Users =
            listOf(
                Auth0User(
                    user_id = "auth0|123",
                    email = "test@example.com",
                ),
                Auth0User(
                    user_id = "auth0|456",
                    email = "user2@example.com",
                ),
            )
    }

    @Test
    fun `searchUsers should return list of users when found`() {
        // Given
        val search = "test"
        whenever(auth0ManagementService.searchUsersByEmailFragment(search))
            .thenReturn(auth0Users)

        // When
        val result = auth0UserController.searchUsers(search)

        // Then
        result.statusCode.value() shouldBeEqualTo 200
        result.body?.size shouldBeEqualTo 2
        result.body?.get(0)?.id shouldBeEqualTo "auth0|123"
        result.body?.get(0)?.email shouldBeEqualTo "test@example.com"
    }

    @Test
    fun `searchUsers should filter out users without email or user_id`() {
        // Given
        val search = "test"
        val usersWithNulls =
            listOf(
                Auth0User(
                    user_id = "auth0|123",
                    email = "test@example.com",
                ),
                Auth0User(
                    user_id = null,
                    email = "invalid@example.com",
                ),
                Auth0User(
                    user_id = "auth0|456",
                    email = null,
                ),
            )
        whenever(auth0ManagementService.searchUsersByEmailFragment(search))
            .thenReturn(usersWithNulls)

        // When
        val result = auth0UserController.searchUsers(search)

        // Then
        result.statusCode.value() shouldBeEqualTo 200
        result.body?.size shouldBeEqualTo 1
        result.body?.get(0)?.id shouldBeEqualTo "auth0|123"
    }

    @Test
    fun `searchUsers should return empty list when no users found`() {
        // Given
        val search = "nonexistent"
        whenever(auth0ManagementService.searchUsersByEmailFragment(search))
            .thenReturn(emptyList())

        // When
        val result = auth0UserController.searchUsers(search)

        // Then
        result.statusCode.value() shouldBeEqualTo 200
        result.body?.size shouldBeEqualTo 0
    }
}
