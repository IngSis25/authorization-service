package authorization.entities

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class CreateUserTest {
    @Test
    fun `CreateUser should create correctly`() {
        // Given
        val email = "test@example.com"

        // When
        val createUser = CreateUser(email)

        // Then
        createUser.email shouldBeEqualTo email
    }
}
