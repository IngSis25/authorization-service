package authorization.dtos

import authorization.services.Auth0Service
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class DTOsTest {
    @Test
    fun `UserDTO constructor from Auth0User should map correctly`() {
        // Given
        val auth0User =
            Auth0Service.Auth0User(
                user_id = "auth0|123",
                email = "test@example.com",
                name = "Test User",
            )

        // When
        val userDTO = UserDTO(auth0User)

        // Then
        userDTO.id shouldBeEqualTo "auth0|123"
        userDTO.email shouldBeEqualTo "test@example.com"
        userDTO.auth0Id shouldBeEqualTo "auth0|123"
        userDTO.name shouldBeEqualTo "Test User"
    }

    @Test
    fun `UserDTO constructor from Auth0User with null values should map correctly`() {
        // Given
        val auth0User =
            Auth0Service.Auth0User(
                user_id = "auth0|123",
                email = null,
                name = null,
            )

        // When
        val userDTO = UserDTO(auth0User)

        // Then
        userDTO.id shouldBeEqualTo "auth0|123"
        userDTO.email shouldBeEqualTo null
        userDTO.auth0Id shouldBeEqualTo "auth0|123"
        userDTO.name shouldBeEqualTo null
    }

    @Test
    fun `AddSnippetRequest should create correctly`() {
        // Given
        val email = "test@example.com"
        val role = "Guest"

        // When
        val request = AddSnippetRequest(email, role)

        // Then
        request.email shouldBeEqualTo email
        request.role shouldBeEqualTo role
    }
}
