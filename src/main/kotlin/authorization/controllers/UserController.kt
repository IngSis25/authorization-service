package authorization.controllers

import authorization.dtos.AddSnippetRequest
import authorization.dtos.UserDTO
import authorization.dtos.UserSnippetDto
import authorization.entities.CreateUser
import authorization.errors.UserNotFoundException
import authorization.routes.UserControllerRoutes
import authorization.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
    @Autowired(required = false)
    private val jwtDecoder: JwtDecoder?,
) : UserControllerRoutes {
    @PostMapping("/")
    override fun create(
        @RequestHeader("Authorization") token: String,
        @RequestBody createUser: CreateUser,
    ): Mono<ResponseEntity<UserDTO>> {
        // Validar token (no usamos el auth0Id acá, solo validamos que el token sea válido)
        jwtDecoder?.decode(token.removePrefix("Bearer "))?.claims?.get("sub") as? String
            ?: throw IllegalStateException("JwtDecoder not available")

        // Este endpoint solo verifica si el usuario existe en Auth0
        return userService.getByEmail(createUser.email)
            .map { user ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(UserDTO(user))
            }
            .onErrorResume { e: Throwable ->
                if (e is UserNotFoundException) {
                    Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build())
                } else {
                    Mono.error(e)
                }
            }
    }

    @GetMapping("/auth0/{auth0Id}")
    override fun getUserByAuth0Id(
        @PathVariable auth0Id: String,
    ): Mono<ResponseEntity<UserDTO>> {
        return userService.getByAuthId(auth0Id)
            .map { user -> ResponseEntity.ok(UserDTO(user)) }
            .onErrorResume { e: Throwable ->
                if (e is UserNotFoundException) {
                    Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build())
                } else {
                    Mono.error(e)
                }
            }
    }

    @GetMapping("/")
    override fun getAllUsers(): Mono<ResponseEntity<List<UserDTO>>> {
        return userService.getAllUsers()
            .map { users ->
                val usersDTO = users.map { user -> UserDTO(user) }
                ResponseEntity.ok(usersDTO)
            }
            .onErrorResume { Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()) }
    }

    @GetMapping("/get-user-snippets/{auth0Id}")
    override fun getUserSnippets(
        @PathVariable auth0Id: String,
    ): ResponseEntity<List<UserSnippetDto>> {
        val snippets = userService.getSnippetsOfUser(auth0Id)
        return ResponseEntity.ok(snippets)
    }

    // NUEVO CONTRATO: usamos snippetId en el path y role en el body.
    // El usuario se obtiene SIEMPRE del token (sub).
    @PostMapping("/add-snippet/{snippetId}")
    override fun addSnippetToUser(
        @RequestHeader("Authorization") token: String,
        @PathVariable snippetId: Long,
        @RequestBody addSnippet: AddSnippetRequest,
    ): Mono<ResponseEntity<String>> {
        val auth0Id = extractAuth0Id(token)

        println(">>> [AUTH] add-snippet auth0Id=$auth0Id snippetId=$snippetId role=${addSnippet.role}")

        return userService.addSnippetToUser(
            auth0Id = auth0Id,
            snippetId = snippetId,
            role = addSnippet.role,
        ).onErrorResume { e: Throwable ->
            println(">>> [AUTH] Error in addSnippetToUser: ${e.message}")
            Mono.just(ResponseEntity.badRequest().body("Error adding snippet to user: ${e.message}"))
        }
    }

    @PostMapping("/check-owner/{snippetId}")
    override fun checkIfOwner(
        @RequestHeader("Authorization") token: String,
        @PathVariable snippetId: Long,
    ): Mono<ResponseEntity<String>> {
        val auth0Id = extractAuth0Id(token)
        return userService.checkIfOwner(snippetId, auth0Id)
    }

    @GetMapping("/validate")
    override fun validate(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<String> {
        return try {
            val auth0Id = extractAuth0Id(token)
            ResponseEntity.ok(auth0Id)
        } catch (e: Exception) {
            println("Error validating token: ${e.message}")
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("")
        }
    }

    @GetMapping("/{email}")
    override fun getUserByEmail(
        @PathVariable email: String,
    ): Mono<ResponseEntity<UserDTO>> {
        return userService.getByEmail(email)
            .map { user -> ResponseEntity.ok(UserDTO(user)) }
            .onErrorResume { e: Throwable ->
                if (e is UserNotFoundException) {
                    Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build())
                } else {
                    Mono.error(e)
                }
            }
    }

    private fun extractAuth0Id(token: String): String {
        val actualToken = token.removePrefix("Bearer ").trim()
        val decoder = jwtDecoder ?: throw IllegalStateException("JwtDecoder not available")
        val jwt = decoder.decode(actualToken)
        return jwt.claims["sub"] as? String
            ?: throw IllegalStateException("Token does not contain 'sub' claim")
    }
}
