package authorization.services

import authorization.dtos.UserSnippetDto
import authorization.entities.UserSnippet
import authorization.errors.UserNotFoundException
import authorization.repositories.UserSnippetsRepository
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono

@ExtendWith(MockitoExtension::class)
class UserServiceTest {
    @Mock
    private lateinit var auth0Service: Auth0Service

    @Mock
    private lateinit var userSnippetsRepository: UserSnippetsRepository

    @InjectMocks
    private lateinit var userService: UserService

    private lateinit var auth0User: Auth0Service.Auth0User
    private lateinit var ownerSnippet: UserSnippet
    private lateinit var guestSnippet: UserSnippet

    @BeforeEach
    fun setUp() {
        auth0User =
            Auth0Service.Auth0User(
                user_id = "auth0|123",
                email = "test@example.com",
                name = "Test User",
            )

        ownerSnippet =
            UserSnippet(
                id = 1L,
                auth0Id = auth0User.user_id,
                snippetId = 100L,
                role = "Owner",
            )

        guestSnippet =
            UserSnippet(
                id = 2L,
                auth0Id = auth0User.user_id,
                snippetId = 100L,
                role = "Guest",
            )
    }

    @Test
    fun `getByEmail should return user when found`() {
        // Given
        val email = "test@example.com"
        whenever(auth0Service.getUserByEmail(email)).thenReturn(Mono.just(auth0User))

        // When
        val result = userService.getByEmail(email).block()

        // Then
        result shouldBeEqualTo auth0User
        verify(auth0Service).getUserByEmail(email)
    }

    @Test
    fun `getByEmail should throw UserNotFoundException when user not found`() {
        // Given
        val email = "notfound@example.com"
        // Simulamos "no encontrado" devolviendo Mono.empty()
        whenever(auth0Service.getUserByEmail(email)).thenReturn(Mono.empty())

        // When - el .block() debe disparar la excepción
        val exception =
            assertThrows<RuntimeException> {
                userService.getByEmail(email).block()
            }

        // Then - verificamos que la causa raíz es UserNotFoundException
        val cause = exception.cause
        assertTrue(cause is UserNotFoundException, "Expected UserNotFoundException but got ${cause?.javaClass}")

        verify(auth0Service).getUserByEmail(email)
    }

    @Test
    fun `getByAuthId should return user when found`() {
        // Given
        val auth0Id = "auth0|123"
        whenever(auth0Service.getUserByAuth0Id(auth0Id)).thenReturn(Mono.just(auth0User))

        // When
        val result = userService.getByAuthId(auth0Id).block()

        // Then
        result shouldBeEqualTo auth0User
        verify(auth0Service).getUserByAuth0Id(auth0Id)
    }

    @Test
    fun `getByAuthId should throw UserNotFoundException when user not found`() {
        // Given
        val auth0Id = "auth0|999"
        whenever(auth0Service.getUserByAuth0Id(auth0Id)).thenReturn(Mono.error(UserNotFoundException("not found")))

        // When - Reactor envuelve las excepciones en RuntimeException
        val exception =
            assertThrows<RuntimeException> {
                userService.getByAuthId(auth0Id).block()
            }

        // Then - verificamos que la causa raíz es UserNotFoundException
        val cause = exception.cause
        assertTrue(cause is UserNotFoundException, "Expected UserNotFoundException but got ${cause?.javaClass}")

        verify(auth0Service).getUserByAuth0Id(auth0Id)
    }

    @Test
    fun `getAllUsers should return list of all users`() {
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
        whenever(auth0Service.getAllUsers()).thenReturn(Mono.just(users))

        // When
        val result = userService.getAllUsers().block()

        // Then
        result?.size shouldBeEqualTo 2
        result shouldBeEqualTo users
        verify(auth0Service).getAllUsers()
    }

    @Test
    fun `getSnippetsOfUser should return list of user snippets`() {
        // Given
        val auth0Id = auth0User.user_id
        val snippets = listOf(ownerSnippet)
        whenever(userSnippetsRepository.findByAuth0Id(auth0Id)).thenReturn(snippets)

        // When
        val result: List<UserSnippetDto> = userService.getSnippetsOfUser(auth0Id)

        // Then
        result.size shouldBeEqualTo 1
        result[0].snippetId shouldBeEqualTo ownerSnippet.snippetId
        result[0].role shouldBeEqualTo ownerSnippet.role
        verify(userSnippetsRepository).findByAuth0Id(auth0Id)
    }

    @Test
    fun `getSnippetsOfUser should throw UserNotFoundException when repository fails`() {
        // Given
        val auth0Id = "auth0|invalid"
        whenever(userSnippetsRepository.findByAuth0Id(auth0Id)).thenThrow(RuntimeException("DB error"))

        // When - tu implementación actual captura cualquier excepción y lanza UserNotFoundException
        assertThrows<UserNotFoundException> {
            userService.getSnippetsOfUser(auth0Id)
        }
    }

    @Test
    fun `addSnippetToUser should add snippet when relation does not exist`() {
        // Given
        val auth0Id = auth0User.user_id
        val snippetId = 200L
        val role = "Guest"
        val newSnippet =
            UserSnippet(
                id = 3L,
                auth0Id = auth0Id,
                snippetId = snippetId,
                role = role,
            )

        whenever(userSnippetsRepository.findByAuth0IdAndSnippetId(auth0Id, snippetId)).thenReturn(null)
        whenever(userSnippetsRepository.save(any<UserSnippet>())).thenReturn(newSnippet)

        // When
        val response = userService.addSnippetToUser(auth0Id, snippetId, role).block()!!

        // Then
        response.statusCode shouldBeEqualTo HttpStatus.OK
        response.body shouldBeEqualTo "Snippet added to user"
        verify(userSnippetsRepository).findByAuth0IdAndSnippetId(auth0Id, snippetId)
        verify(userSnippetsRepository).save(any())
    }

    @Test
    fun `addSnippetToUser should return ok when relation already exists`() {
        // Given
        val auth0Id = auth0User.user_id
        val snippetId = 100L
        val role = "Guest"

        whenever(
            userSnippetsRepository.findByAuth0IdAndSnippetId(auth0Id, snippetId),
        ).thenReturn(ownerSnippet)

        // When
        val response = userService.addSnippetToUser(auth0Id, snippetId, role).block()!!

        // Then
        response.statusCode shouldBeEqualTo HttpStatus.OK
        response.body shouldBeEqualTo "User already has this snippet."
        verify(userSnippetsRepository).findByAuth0IdAndSnippetId(auth0Id, snippetId)
        verify(userSnippetsRepository, never()).save(any())
    }

    @Test
    fun `addSnippetToUser should return bad request when repository throws exception`() {
        // Given
        val auth0Id = "auth0|invalid"
        val snippetId = 100L
        val role = "Owner"

        whenever(userSnippetsRepository.findByAuth0IdAndSnippetId(auth0Id, snippetId))
            .thenThrow(RuntimeException("Database error"))

        // When
        val response = userService.addSnippetToUser(auth0Id, snippetId, role).block()!!

        // Then
        response.statusCode shouldBeEqualTo HttpStatus.BAD_REQUEST
        response.body?.contains("Error adding snippet to user") shouldBeEqualTo true
        verify(userSnippetsRepository).findByAuth0IdAndSnippetId(auth0Id, snippetId)
        verify(userSnippetsRepository, never()).save(any())
    }

    @Test
    fun `checkIfOwner should return ok when user is owner`() {
        // Given
        val auth0Id = auth0User.user_id
        val snippetId = 100L

        whenever(userSnippetsRepository.findByAuth0Id(auth0Id)).thenReturn(listOf(ownerSnippet))

        // When
        val response = userService.checkIfOwner(snippetId, auth0Id).block()!!

        // Then
        response.statusCode shouldBeEqualTo HttpStatus.OK
        response.body shouldBeEqualTo "User is the owner of the snippet"
        verify(userSnippetsRepository).findByAuth0Id(auth0Id)
    }

    @Test
    fun `checkIfOwner should return bad request when user is not owner`() {
        // Given
        val auth0Id = auth0User.user_id
        val snippetId = 100L

        whenever(userSnippetsRepository.findByAuth0Id(auth0Id)).thenReturn(listOf(guestSnippet))

        // When
        val response = userService.checkIfOwner(snippetId, auth0Id).block()!!

        // Then
        response.statusCode shouldBeEqualTo HttpStatus.BAD_REQUEST
        response.body shouldBeEqualTo "User is not the owner of the snippet"
    }

    @Test
    fun `checkIfOwner should return bad request when snippet does not exist`() {
        // Given
        val auth0Id = auth0User.user_id
        val snippetId = 999L

        whenever(userSnippetsRepository.findByAuth0Id(auth0Id)).thenReturn(emptyList())

        // When
        val response = userService.checkIfOwner(snippetId, auth0Id).block()!!

        // Then
        response.statusCode shouldBeEqualTo HttpStatus.BAD_REQUEST
        response.body shouldBeEqualTo "Snippet of id provided doesn't exist"
    }
}
