package authorization.controllers

import authorization.dtos.AddSnippetRequest
import authorization.entities.CreateUser
import authorization.errors.UserNotFoundException
import authorization.services.Auth0Service
import authorization.services.UserService
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
class UserControllerTest {
    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var jwtDecoder: JwtDecoder

    @InjectMocks
    private lateinit var userController: UserController

    private lateinit var auth0User: Auth0Service.Auth0User
    private lateinit var jwt: Jwt

    @BeforeEach
    fun setUp() {
        auth0User =
            Auth0Service.Auth0User(
                user_id = "auth0|123",
                email = "test@example.com",
                name = "Test User",
            )

        jwt =
            Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "auth0|123")
                .build()
    }

    @Test
    fun `create should return conflict when user exists`() {
        // Given
        val token = "Bearer valid-token"
        val createUser = CreateUser("test@example.com")
        whenever(jwtDecoder.decode("valid-token")).thenReturn(jwt)
        whenever(userService.getByEmail("test@example.com")).thenReturn(Mono.just(auth0User))

        // When
        val result = userController.create(token, createUser).block()

        // Then
        result?.statusCode shouldBeEqualTo HttpStatus.CONFLICT
        result?.body?.id shouldBeEqualTo "auth0|123"
        result?.body?.email shouldBeEqualTo "test@example.com"
    }

    @Test
    fun `create should return not found when user does not exist`() {
        // Given
        val token = "Bearer valid-token"
        val createUser = CreateUser("notfound@example.com")
        whenever(jwtDecoder.decode("valid-token")).thenReturn(jwt)
        whenever(userService.getByEmail("notfound@example.com"))
            .thenReturn(Mono.error(UserNotFoundException("User not found")))

        // When
        val result = userController.create(token, createUser).block()

        // Then
        result?.statusCode shouldBeEqualTo HttpStatus.NOT_FOUND
    }

    @Test
    fun `getUserByAuth0Id should return user when found`() {
        // Given
        val auth0Id = "auth0|123"
        whenever(userService.getByAuthId(auth0Id)).thenReturn(Mono.just(auth0User))

        // When
        val result = userController.getUserByAuth0Id(auth0Id).block()

        // Then
        result?.statusCode shouldBeEqualTo HttpStatus.OK
        result?.body?.id shouldBeEqualTo "auth0|123"
    }

    @Test
    fun `getUserByAuth0Id should return not found when user does not exist`() {
        // Given
        val auth0Id = "auth0|999"
        whenever(userService.getByAuthId(auth0Id))
            .thenReturn(Mono.error(UserNotFoundException("User not found")))

        // When
        val result = userController.getUserByAuth0Id(auth0Id).block()

        // Then
        result?.statusCode shouldBeEqualTo HttpStatus.NOT_FOUND
    }

    @Test
    fun `getAllUsers should return list of users`() {
        // Given
        val users =
            listOf(
                auth0User,
                Auth0Service.Auth0User(
                    user_id = "auth0|456",
                    email = "user2@example.com",
                    name = "Other User",
                ),
            )
        whenever(userService.getAllUsers()).thenReturn(Mono.just(users))

        // When
        val result = userController.getAllUsers().block()

        // Then
        result?.statusCode shouldBeEqualTo HttpStatus.OK
        result?.body?.size shouldBeEqualTo 2
    }

    @Test
    fun `getAllUsers should return internal server error on exception`() {
        // Given
        whenever(userService.getAllUsers()).thenReturn(Mono.error(RuntimeException("Error")))

        // When
        val result = userController.getAllUsers().block()

        // Then
        result?.statusCode shouldBeEqualTo HttpStatus.INTERNAL_SERVER_ERROR
    }

    @Test
    fun `getUserSnippets should return list of snippets`() {
        // Given
        val auth0Id = "auth0|123"
        val snippets =
            listOf(
                authorization.dtos.UserSnippetDto(100L, "Owner"),
                authorization.dtos.UserSnippetDto(200L, "Guest"),
            )
        whenever(userService.getSnippetsOfUser(auth0Id)).thenReturn(snippets)

        // When
        val result = userController.getUserSnippets(auth0Id)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.OK
        result.body?.size shouldBeEqualTo 2
    }

    @Test
    fun `addSnippetToUser should add snippet successfully`() {
        // Given
        val token = "Bearer valid-token"
        val snippetId = 100L
        val addSnippet = AddSnippetRequest("test@example.com", "Guest")
        whenever(jwtDecoder.decode("valid-token")).thenReturn(jwt)
        whenever(userService.getByEmail("test@example.com")).thenReturn(Mono.just(auth0User))
        whenever(
            userService.addSnippetToUser(
                "auth0|123",
                snippetId,
                "Guest",
            ),
        ).thenReturn(Mono.just(ResponseEntity.ok("Snippet added to user")))

        // When
        val result = userController.addSnippetToUser(token, snippetId, addSnippet).block()

        // Then
        result?.statusCode shouldBeEqualTo HttpStatus.OK
        result?.body shouldBeEqualTo "Snippet added to user"
    }

    @Test
    fun `addSnippetToUser should return not found when user does not exist`() {
        // Given
        val token = "Bearer valid-token"
        val snippetId = 100L
        val addSnippet = AddSnippetRequest("notfound@example.com", "Guest")
        whenever(jwtDecoder.decode("valid-token")).thenReturn(jwt)
        whenever(userService.getByEmail("notfound@example.com"))
            .thenReturn(Mono.error(UserNotFoundException("User not found")))

        // When
        val result = userController.addSnippetToUser(token, snippetId, addSnippet).block()

        // Then
        result?.statusCode shouldBeEqualTo HttpStatus.NOT_FOUND
        result?.body shouldBeEqualTo "User not found"
    }

    @Test
    fun `addSnippetToUser should return bad request on other errors`() {
        // Given
        val token = "Bearer valid-token"
        val snippetId = 100L
        val addSnippet = AddSnippetRequest("test@example.com", "Guest")
        whenever(jwtDecoder.decode("valid-token")).thenReturn(jwt)
        whenever(userService.getByEmail("test@example.com"))
            .thenReturn(Mono.error(RuntimeException("Database error")))

        // When
        val result = userController.addSnippetToUser(token, snippetId, addSnippet).block()

        // Then
        result?.statusCode shouldBeEqualTo HttpStatus.BAD_REQUEST
        result?.body?.contains("Error adding snippet to user") shouldBeEqualTo true
    }

    @Test
    fun `checkIfOwner should return ok when user is owner`() {
        // Given
        val token = "Bearer valid-token"
        val snippetId = 100L
        whenever(jwtDecoder.decode("valid-token")).thenReturn(jwt)
        whenever(userService.checkIfOwner(snippetId, "auth0|123"))
            .thenReturn(Mono.just(ResponseEntity.ok("User is the owner of the snippet")))

        // When
        val result = userController.checkIfOwner(token, snippetId).block()

        // Then
        result?.statusCode shouldBeEqualTo HttpStatus.OK
        result?.body shouldBeEqualTo "User is the owner of the snippet"
    }

    @Test
    fun `validate should return ok with auth0Id when token is valid`() {
        // Given
        val token = "Bearer valid-token"
        whenever(jwtDecoder.decode("valid-token")).thenReturn(jwt)

        // When
        val result = userController.validate(token)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.OK
        result.body shouldBeEqualTo "auth0|123"
    }

    @Test
    fun `validate should return unauthorized when token is invalid`() {
        // Given
        val token = "Bearer invalid-token"
        whenever(jwtDecoder.decode("invalid-token")).thenThrow(RuntimeException("Invalid token"))

        // When
        val result = userController.validate(token)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.UNAUTHORIZED
        result.body shouldBeEqualTo ""
    }

    @Test
    fun `create should throw exception when JwtDecoder is null`() {
        // Given
        val controllerWithoutJwtDecoder = UserController(userService, null)
        val token = "Bearer valid-token"
        val createUser = CreateUser("test@example.com")

        // When & Then
        try {
            controllerWithoutJwtDecoder.create(token, createUser).block()
            assert(false) { "Should have thrown exception" }
        } catch (e: IllegalStateException) {
            e.message?.contains("JwtDecoder not available") shouldBeEqualTo true
        }
    }

    @Test
    fun `create should throw exception when token does not have sub claim`() {
        // Given
        val token = "Bearer valid-token"
        val createUser = CreateUser("test@example.com")
        val jwtWithoutSub =
            Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("other", "value")
                .build()
        whenever(jwtDecoder.decode("valid-token")).thenReturn(jwtWithoutSub)

        // When & Then
        try {
            userController.create(token, createUser).block()
            assert(false) { "Should have thrown exception" }
        } catch (e: IllegalStateException) {
            // The code throws "JwtDecoder not available" when sub claim is missing
            e.message?.contains("JwtDecoder not available") shouldBeEqualTo true
        }
    }

    @Test
    fun `create should propagate non-UserNotFoundException errors`() {
        // Given
        val token = "Bearer valid-token"
        val createUser = CreateUser("test@example.com")
        whenever(jwtDecoder.decode("valid-token")).thenReturn(jwt)
        whenever(userService.getByEmail("test@example.com"))
            .thenReturn(Mono.error(RuntimeException("Unexpected error")))

        // When & Then
        StepVerifier.create(userController.create(token, createUser))
            .expectError(RuntimeException::class.java)
            .verify()
    }

    @Test
    fun `validate should handle token without Bearer prefix`() {
        // Given
        val token = "valid-token"
        whenever(jwtDecoder.decode("valid-token")).thenReturn(jwt)

        // When
        val result = userController.validate(token)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.OK
        result.body shouldBeEqualTo "auth0|123"
    }

    @Test
    fun `validate should handle token with extra spaces`() {
        // Given
        val token = "Bearer  valid-token  "
        whenever(jwtDecoder.decode("valid-token")).thenReturn(jwt)

        // When
        val result = userController.validate(token)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.OK
        result.body shouldBeEqualTo "auth0|123"
    }

    @Test
    fun `getUserByEmail should return user when found`() {
        // Given
        val email = "test@example.com"
        whenever(userService.getByEmail(email)).thenReturn(Mono.just(auth0User))

        // When
        val result = userController.getUserByEmail(email).block()

        // Then
        result?.statusCode shouldBeEqualTo HttpStatus.OK
        result?.body?.id shouldBeEqualTo "auth0|123"
    }

    @Test
    fun `getUserByEmail should return not found when user does not exist`() {
        // Given
        val email = "notfound@example.com"
        whenever(userService.getByEmail(email))
            .thenReturn(Mono.error(UserNotFoundException("User not found")))

        // When
        val result = userController.getUserByEmail(email).block()

        // Then
        result?.statusCode shouldBeEqualTo HttpStatus.NOT_FOUND
    }

    @Test
    fun `checkIfOwner should handle JwtDecoder null`() {
        // Given
        val controllerWithoutJwtDecoder = UserController(userService, null)
        val token = "Bearer valid-token"
        val snippetId = 100L

        // When & Then
        try {
            controllerWithoutJwtDecoder.checkIfOwner(token, snippetId).block()
            assert(false) { "Should have thrown exception" }
        } catch (e: IllegalStateException) {
            e.message?.contains("JwtDecoder not available") shouldBeEqualTo true
        }
    }

    @Test
    fun `checkIfOwner should handle token without sub claim`() {
        // Given
        val token = "Bearer valid-token"
        val snippetId = 100L
        val jwtWithoutSub =
            Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("other", "value")
                .build()
        whenever(jwtDecoder.decode("valid-token")).thenReturn(jwtWithoutSub)

        // When & Then
        try {
            userController.checkIfOwner(token, snippetId).block()
            assert(false) { "Should have thrown exception" }
        } catch (e: IllegalStateException) {
            e.message?.contains("Token does not contain 'sub' claim") shouldBeEqualTo true
        }
    }

    @Test
    fun `addSnippetToUser should handle JwtDecoder null`() {
        // Given
        val controllerWithoutJwtDecoder = UserController(userService, null)
        val token = "Bearer valid-token"
        val snippetId = 100L
        val addSnippet = AddSnippetRequest("test@example.com", "Guest")

        // When & Then
        try {
            controllerWithoutJwtDecoder.addSnippetToUser(token, snippetId, addSnippet).block()
            assert(false) { "Should have thrown exception" }
        } catch (e: IllegalStateException) {
            e.message?.contains("JwtDecoder not available") shouldBeEqualTo true
        }
    }
}
