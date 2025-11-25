package services

import entities.Author
import entities.UserSnippet
import errors.UserNotFoundException
import org.amshove.kluent.shouldBeEqualTo
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
import repositories.UserRepository
import repositories.UserSnippetsRepository
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserServiceTest {
    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var userSnippetsRepository: UserSnippetsRepository

    @InjectMocks
    private lateinit var userService: UserService

    private lateinit var author: Author
    private lateinit var userSnippet: UserSnippet

    @BeforeEach
    fun setUp() {
        author = Author(id = 1L, email = "test@example.com", auth0Id = "auth0|123")
        userSnippet = UserSnippet(id = 1L, author = author, snippetId = 100L, role = "Owner")
    }

    @Test
    fun `getByEmail should return user when found`() {
        // Given
        val email = "test@example.com"
        whenever(userRepository.findByEmail(email)).thenReturn(author)

        // When
        val result = userService.getByEmail(email)

        // Then
        result shouldBeEqualTo author
        verify(userRepository).findByEmail(email)
    }

    @Test
    fun `getByEmail should throw UserNotFoundException when user not found`() {
        // Given
        val email = "notfound@example.com"
        whenever(userRepository.findByEmail(email)).thenReturn(null)

        // When/Then
        assertThrows<UserNotFoundException> {
            userService.getByEmail(email)
        }
        verify(userRepository).findByEmail(email)
    }

    @Test
    fun `getById should return user when found`() {
        // Given
        val id = 1L
        whenever(userRepository.findById(id)).thenReturn(Optional.of(author))

        // When
        val result = userService.getById(id)

        // Then
        result shouldBeEqualTo author
        verify(userRepository).findById(id)
    }

    @Test
    fun `getById should throw UserNotFoundException when user not found`() {
        // Given
        val id = 999L
        whenever(userRepository.findById(id)).thenReturn(Optional.empty())

        // When/Then
        assertThrows<UserNotFoundException> {
            userService.getById(id)
        }
        verify(userRepository).findById(id)
    }

    @Test
    fun `getByAuthId should return user when found`() {
        // Given
        val auth0Id = "auth0|123"
        whenever(userRepository.findByAuth0Id(auth0Id)).thenReturn(author)

        // When
        val result = userService.getByAuthId(auth0Id)

        // Then
        result shouldBeEqualTo author
        verify(userRepository).findByAuth0Id(auth0Id)
    }

    @Test
    fun `getByAuthId should throw UserNotFoundException when user not found`() {
        // Given
        val auth0Id = "auth0|999"
        whenever(userRepository.findByAuth0Id(auth0Id)).thenReturn(null)

        // When/Then
        assertThrows<UserNotFoundException> {
            userService.getByAuthId(auth0Id)
        }
        verify(userRepository).findByAuth0Id(auth0Id)
    }

    @Test
    fun `createUser should create and return new user`() {
        // Given
        val email = "new@example.com"
        val auth0Id = "auth0|456"
        val savedAuthor = Author(id = 2L, email = email, auth0Id = auth0Id)
        whenever(userRepository.save(any<Author>())).thenReturn(savedAuthor)

        // When
        val result = userService.createUser(email, auth0Id)

        // Then
        result.email shouldBeEqualTo email
        result.auth0Id shouldBeEqualTo auth0Id
        verify(userRepository).save(any<Author>())
    }

    @Test
    fun `getAllUsers should return list of all users`() {
        // Given
        val users = listOf(author, Author(id = 2L, email = "user2@example.com", auth0Id = "auth0|789"))
        whenever(userRepository.findAll()).thenReturn(users)

        // When
        val result = userService.getAllUsers()

        // Then
        result.size shouldBeEqualTo 2
        result shouldBeEqualTo users
        verify(userRepository).findAll()
    }

    @Test
    fun `getSnippetsOfUser should return list of user snippets`() {
        // Given
        val auth0Id = "auth0|123"
        val snippets = listOf(userSnippet)
        whenever(userRepository.findByAuth0Id(auth0Id)).thenReturn(author)
        whenever(userSnippetsRepository.findByAuthorId(author.id!!)).thenReturn(snippets)

        // When
        val result = userService.getSnippetsOfUser(auth0Id)

        // Then
        result.size shouldBeEqualTo 1
        result[0].snippetId shouldBeEqualTo 100L
        result[0].role shouldBeEqualTo "Owner"
        verify(userRepository).findByAuth0Id(auth0Id)
        verify(userSnippetsRepository).findByAuthorId(author.id!!)
    }

    @Test
    fun `getSnippetsOfUser should throw UserNotFoundException when user not found`() {
        // Given
        val auth0Id = "auth0|999"
        whenever(userRepository.findByAuth0Id(auth0Id)).thenReturn(null)

        // When/Then
        assertThrows<UserNotFoundException> {
            userService.getSnippetsOfUser(auth0Id)
        }
    }

    @Test
    fun `updateUser should update and return user`() {
        // Given
        val updatedAuthor = author.copy(email = "updated@example.com")
        whenever(userRepository.findById(author.id!!)).thenReturn(Optional.of(author))
        whenever(userRepository.save(any<Author>())).thenReturn(updatedAuthor)

        // When
        val result = userService.updateUser(updatedAuthor)

        // Then
        result?.email shouldBeEqualTo "updated@example.com"
        verify(userRepository).findById(author.id!!)
        verify(userRepository).save(any<Author>())
    }

    @Test
    fun `updateUser should throw UserNotFoundException when user not found`() {
        // Given
        val nonExistentAuthor = Author(id = 999L, email = "notfound@example.com", auth0Id = "auth0|999")
        whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

        // When/Then
        assertThrows<UserNotFoundException> {
            userService.updateUser(nonExistentAuthor)
        }
        verify(userRepository).findById(999L)
        verify(userRepository, never()).save(any<Author>())
    }

    @Test
    fun `addSnippetToUser should add snippet when relation does not exist`() {
        // Given
        val email = "test@example.com"
        val snippetId = 200L
        val role = "Guest"
        val newSnippet = UserSnippet(id = 3L, author = author, snippetId = snippetId, role = role)
        whenever(userRepository.findByEmail(email)).thenReturn(author)
        whenever(userSnippetsRepository.findByAuthorId(author.id!!)).thenReturn(emptyList())
        whenever(userSnippetsRepository.save(any<UserSnippet>())).thenReturn(newSnippet)

        // When
        val result = userService.addSnippetToUser(email, snippetId, role)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.OK
        result.body shouldBeEqualTo "Snippet added to user"
        verify(userRepository).findByEmail(email)
        verify(userSnippetsRepository).save(any<UserSnippet>())
    }

    @Test
    fun `addSnippetToUser should return ok when relation already exists`() {
        // Given
        val email = "test@example.com"
        val snippetId = 100L
        val role = "Guest"
        whenever(userRepository.findByEmail(email)).thenReturn(author)
        whenever(userSnippetsRepository.findByAuthorId(author.id!!)).thenReturn(listOf(userSnippet))

        // When
        val result = userService.addSnippetToUser(email, snippetId, role)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.OK
        result.body shouldBeEqualTo "User already has this snippet."
        verify(userRepository).findByEmail(email)
        verify(userSnippetsRepository, never()).save(any())
    }

    @Test
    fun `checkIfOwner should return ok when user is owner`() {
        // Given
        val email = "test@example.com"
        val snippetId = 100L
        whenever(userRepository.findByEmail(email)).thenReturn(author)
        whenever(userSnippetsRepository.findByAuthorId(author.id!!)).thenReturn(listOf(userSnippet))

        // When
        val result = userService.checkIfOwner(snippetId, email)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.OK
        result.body shouldBeEqualTo "User is the owner of the snippet"
        verify(userRepository).findByEmail(email)
        verify(userSnippetsRepository).findByAuthorId(author.id!!)
    }

    @Test
    fun `checkIfOwner should return bad request when user is not owner`() {
        // Given
        val email = "test@example.com"
        val snippetId = 100L
        val guestSnippet = UserSnippet(id = 2L, author = author, snippetId = snippetId, role = "Guest")
        whenever(userRepository.findByEmail(email)).thenReturn(author)
        whenever(userSnippetsRepository.findByAuthorId(author.id!!)).thenReturn(listOf(guestSnippet))

        // When
        val result = userService.checkIfOwner(snippetId, email)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.BAD_REQUEST
        result.body shouldBeEqualTo "User is not the owner of the snippet"
    }

    @Test
    fun `checkIfOwner should return bad request when snippet does not exist`() {
        // Given
        val email = "test@example.com"
        val snippetId = 999L
        whenever(userRepository.findByEmail(email)).thenReturn(author)
        whenever(userSnippetsRepository.findByAuthorId(author.id!!)).thenReturn(emptyList())

        // When
        val result = userService.checkIfOwner(snippetId, email)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.BAD_REQUEST
        result.body shouldBeEqualTo "Snippet of id provided doesn't exist"
    }

    @Test
    fun `getSnippetsId should return list of snippet IDs`() {
        // Given
        val userId = 1L
        val snippets = listOf(userSnippet, UserSnippet(id = 2L, author = author, snippetId = 200L, role = "Guest"))
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(author))
        whenever(userSnippetsRepository.findByAuthorId(author.id!!)).thenReturn(snippets)

        // When
        val result = userService.getSnippetsId(userId)

        // Then
        result.statusCode shouldBeEqualTo HttpStatus.OK
        result.body?.size shouldBeEqualTo 2
        result.body?.contains(1L) shouldBeEqualTo true
        result.body?.contains(2L) shouldBeEqualTo true
        verify(userRepository).findById(userId)
        verify(userSnippetsRepository).findByAuthorId(author.id!!)
    }
}
