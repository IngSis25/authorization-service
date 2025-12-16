package authorization.request_types

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class TypesTest {
    @Test
    fun `UserSnippet should create correctly`() {
        // Given
        val snippetId = 100L
        val role = "Owner"

        // When
        val userSnippet = UserSnippet(snippetId, role)

        // Then
        userSnippet.snippetId shouldBeEqualTo snippetId
        userSnippet.role shouldBeEqualTo role
    }

    @Test
    fun `CheckRequest should create correctly`() {
        // Given
        val snippetId = 100L
        val email = "test@example.com"

        // When
        val checkRequest = CheckRequest(snippetId, email)

        // Then
        checkRequest.snippetId shouldBeEqualTo snippetId
        checkRequest.email shouldBeEqualTo email
    }
}
