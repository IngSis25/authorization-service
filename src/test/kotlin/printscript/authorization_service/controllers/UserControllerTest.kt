package printscript.authorization_service.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import controllers.UserController
import dtos.UserDTO
import dtos.UserSnippetDto
import entities.Author
import entities.CreateUser
import entities.UserSnippet
import errors.UserNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import request_types.CheckRequest
import request_types.UserSnippet as UserSnippetRequest
import services.UserService
import java.time.Instant
import java.util.HashMap

/**
 * User Story #7: Compartir snippet
 * Como owner de snippet, quiero poder compartir mis snippets con otros usuarios
 * Para que ellos puedan visualizar los snippets que creé.
 */
@WebMvcTest(
    controllers = [UserController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.security.oauth2.resourceserver.jwt.issuer-uri=", "auth0.audience="])
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userService: UserService

    @MockitoBean
    private lateinit var jwtDecoder: JwtDecoder

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var author: Author
    private lateinit var author2: Author
    private lateinit var userSnippet: UserSnippet
    private lateinit var jwt: Jwt

    @BeforeEach
    fun setUp() {
        author = Author(id = 1L, email = "owner@example.com", auth0Id = "auth0|123")
        author2 = Author(id = 2L, email = "guest@example.com", auth0Id = "auth0|456")
        userSnippet = UserSnippet(id = 1L, author = author, snippetId = 100L, role = "Owner")

        val claims = HashMap<String, Any>()
        claims["sub"] = "auth0|123"
        jwt = Jwt("token", Instant.now(), Instant.now().plusSeconds(3600), mapOf("alg" to "RS256"), claims)
    }

    // ========== User Story #7: Compartir snippet ==========

    @Test
    fun `addSnippetToUser should add snippet to user when sharing`() {
        // Given - User Story #7: Compartir snippet con otro usuario
        val email = "guest@example.com"
        val snippetRequest = UserSnippetRequest(snippetId = 200L, role = "Guest")

        // El controlador devuelve directamente el resultado del servicio
        whenever(userService.addSnippetToUser(email, 200L, "Guest"))
            .thenReturn(ResponseEntity.ok("Snippet added to user"))

        // When/Then
        mockMvc.perform(
            post("/api/user/add-snippet/$email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(snippetRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(content().string("\"Snippet added to user\""))
    }

    @Test
    fun `getUserSnippets should return list of snippets for user`() {
        // Given - User Story #7: Ver snippets compartidos con un usuario
        val userId = "auth0|123"
        val snippets = listOf(
            UserSnippetDto(snippetId = 100L, role = "Owner"),
            UserSnippetDto(snippetId = 200L, role = "Guest"),
        )

        whenever(userService.getSnippetsOfUser(userId)).thenReturn(snippets)

        // When/Then
        mockMvc.perform(get("/api/user/get-user-snippets/$userId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].snippetId").value(100L))
            .andExpect(jsonPath("$[0].role").value("Owner"))
            .andExpect(jsonPath("$[1].snippetId").value(200L))
            .andExpect(jsonPath("$[1].role").value("Guest"))
    }

    @Test
    fun `getAllUsers should return list of all users for sharing`() {
        // Given - User Story #7: Listar usuarios para elegir con quién compartir
        val users = listOf(author, author2)

        whenever(userService.getAllUsers()).thenReturn(users)

        // When/Then - El controlador convierte Author a UserDTO
        mockMvc.perform(get("/api/user/"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].email").value("owner@example.com"))
            .andExpect(jsonPath("$[0].auth0Id").value("auth0|123"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].email").value("guest@example.com"))
            .andExpect(jsonPath("$[1].auth0Id").value("auth0|456"))
    }

    @Test
    fun `checkIfOwner should return ok when user is owner`() {
        // Given - Verificar que un usuario es owner antes de compartir
        val checkRequest = CheckRequest(snippetId = 100L, email = "owner@example.com")

        // El controlador devuelve directamente el resultado del servicio
        whenever(userService.checkIfOwner(100L, "owner@example.com"))
            .thenReturn(ResponseEntity.ok("User is the owner of the snippet"))

        // When/Then - Spring serializa String como JSON
        mockMvc.perform(
            post("/api/user/check-owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(checkRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").value("User is the owner of the snippet"))
    }

    @Test
    fun `checkIfOwner should return bad request when user is not owner`() {
        // Given
        val checkRequest = CheckRequest(snippetId = 100L, email = "guest@example.com")

        whenever(userService.checkIfOwner(100L, "guest@example.com"))
            .thenReturn(ResponseEntity.badRequest().body("User is not the owner of the snippet"))

        // When/Then
        mockMvc.perform(
            post("/api/user/check-owner")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(checkRequest)),
        )
            .andExpect(status().isBadRequest)
    }

    // ========== Gestión de usuarios ==========

    @Test
    fun `create should create new user when user does not exist`() {
        // Given
        val token = "Bearer test-token"
        val createUser = CreateUser(email = "new@example.com")
        val newUser = Author(id = 3L, email = "new@example.com", auth0Id = "auth0|123")

        // El controlador hace token.removePrefix("Bearer "), así que el mock debe recibir "test-token"
        whenever(jwtDecoder.decode("test-token")).thenReturn(jwt)
        whenever(userService.getByEmail("new@example.com")).thenThrow(UserNotFoundException("User not found"))
        whenever(userService.createUser("new@example.com", "auth0|123")).thenReturn(newUser)

        // When/Then
        mockMvc.perform(
            post("/api/user/")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(3L))
            .andExpect(jsonPath("$.email").value("new@example.com"))
    }

    @Test
    fun `create should return conflict when user already exists`() {
        // Given
        val token = "Bearer test-token"
        val createUser = CreateUser(email = "owner@example.com")

        whenever(jwtDecoder.decode("test-token")).thenReturn(jwt)
        whenever(userService.getByEmail("owner@example.com")).thenReturn(author)

        // When/Then
        mockMvc.perform(
            post("/api/user/")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUser)),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.email").value("owner@example.com"))
    }

    @Test
    fun `getUserById should return user`() {
        // Given
        whenever(userService.getById(1L)).thenReturn(author)

        // When/Then
        mockMvc.perform(get("/api/user/get/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.email").value("owner@example.com"))
    }

    @Test
    fun `getUserByAuth0Id should return user`() {
        // Given
        whenever(userService.getByAuthId("auth0|123")).thenReturn(author)

        // When/Then - El controlador convierte Author a UserDTO
        mockMvc.perform(get("/api/user/auth0/auth0|123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.email").value("owner@example.com"))
            .andExpect(jsonPath("$.auth0Id").value("auth0|123"))
    }

    @Test
    fun `getUserByEmail should return user`() {
        // Given
        whenever(userService.getByEmail("owner@example.com")).thenReturn(author)

        // When/Then - El controlador devuelve Author directamente (no UserDTO)
        mockMvc.perform(get("/api/user/owner@example.com"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.email").value("owner@example.com"))
            .andExpect(jsonPath("$.auth0Id").value("auth0|123"))
    }

    @Test
    fun `updateUser should update user`() {
        // Given
        val updatedAuthor = author.copy(email = "updated@example.com")
        whenever(userService.updateUser(any())).thenReturn(updatedAuthor)

        // When/Then
        mockMvc.perform(
            put("/api/user/owner@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedAuthor)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("updated@example.com"))
    }

    @Test
    fun `validate should return user id when token is valid`() {
        // Given
        val token = "Bearer test-token"

        // El controlador hace token.removePrefix("Bearer "), así que el mock debe recibir "test-token"
        whenever(jwtDecoder.decode("test-token")).thenReturn(jwt)
        whenever(userService.getByAuthId("auth0|123")).thenReturn(author)

        // When/Then - El controlador devuelve ResponseEntity.ok(user.id) donde user.id es Long?
        // Se serializa como número JSON
        mockMvc.perform(
            get("/api/user/validate")
                .header("Authorization", token),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").value(1L))
    }

    @Test
    fun `validate should return not found when user does not exist`() {
        // Given
        val token = "Bearer test-token"

        whenever(jwtDecoder.decode("test-token")).thenReturn(jwt)
        whenever(userService.getByAuthId("auth0|123")).thenReturn(null)

        // When/Then
        mockMvc.perform(
            get("/api/user/validate")
                .header("Authorization", token),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `getUserSnippetsId should return list of snippet IDs`() {
        // Given
        val snippetIds = listOf(1L, 2L)
        // El controlador devuelve directamente el resultado del servicio
        whenever(userService.getSnippetsId(1L)).thenReturn(ResponseEntity.ok(snippetIds))

        // When/Then - El contenido es un array JSON de números
        mockMvc.perform(get("/api/user/snippets/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0]").value(1L))
            .andExpect(jsonPath("$[1]").value(2L))
    }
}

