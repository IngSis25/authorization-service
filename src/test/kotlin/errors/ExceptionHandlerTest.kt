package authorization.errors

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ExceptionHandlerTest {
    private val exceptionHandler = ExceptionHandler()

    @Test
    fun `handle should return bad request with exception message`() {
        // Given
        val exception = UserNotFoundException("User not found")

        // When
        val result = exceptionHandler.handle(exception)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.BAD_REQUEST
        result.body shouldBeEqualTo "User not found"
    }

    @Test
    fun `handle should return bad request with custom message`() {
        // Given
        val exception = UserNotFoundException("Custom error message")

        // When
        val result = exceptionHandler.handle(exception)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.BAD_REQUEST
        result.body shouldBeEqualTo "Custom error message"
    }
}
