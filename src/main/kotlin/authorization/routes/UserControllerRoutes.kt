package authorization.routes

import authorization.dtos.AddSnippetRequest
import authorization.dtos.UserDTO
import authorization.dtos.UserSnippetDto
import authorization.entities.CreateUser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import reactor.core.publisher.Mono

/**
 * Interfaz que define las rutas del controlador de usuarios.
 * Esta interfaz documenta los endpoints disponibles en el servicio de autorización.
 */
interface UserControllerRoutes {
    /**
     * Verifica si un usuario existe en Auth0.
     * Requiere un token JWT válido de Auth0.
     */
    @PostMapping("/") // coincide con @PostMapping("/") del UserController
    fun create(
        @RequestHeader("Authorization") token: String,
        @RequestBody createUser: CreateUser,
    ): Mono<ResponseEntity<UserDTO>>

    /**
     * Obtiene un usuario por su Auth0 ID.
     */
    @GetMapping("/auth0/{auth0Id}")
    fun getUserByAuth0Id(
        @PathVariable auth0Id: String,
    ): Mono<ResponseEntity<UserDTO>>

    /**
     * Obtiene un usuario por su email.
     */
    @GetMapping("/{email}")
    fun getUserByEmail(
        @PathVariable email: String,
    ): Mono<ResponseEntity<UserDTO>>

    /**
     * Obtiene todos los usuarios del sistema desde Auth0.
     */
    @GetMapping("/")
    fun getAllUsers(): Mono<ResponseEntity<List<UserDTO>>>

    /**
     * Obtiene los snippets de un usuario por su Auth0 ID.
     */
    @GetMapping("/get-user-snippets/{auth0Id}")
    fun getUserSnippets(
        @PathVariable auth0Id: String,
    ): ResponseEntity<List<UserSnippetDto>>

    /**
     * Agrega un snippet a un usuario con un rol específico.
     * El usuario autenticado se obtiene del token (claim 'sub').
     */
    @PostMapping("/add-snippet/{snippetId}")
    fun addSnippetToUser(
        @RequestHeader("Authorization") token: String,
        @PathVariable snippetId: Long,
        @RequestBody addSnippet: AddSnippetRequest,
    ): Mono<ResponseEntity<String>>

    /**
     * Verifica si el usuario autenticado (token) es el owner de un snippet.
     */
    @PostMapping("/check-owner/{snippetId}")
    fun checkIfOwner(
        @RequestHeader("Authorization") token: String,
        @PathVariable snippetId: Long,
    ): Mono<ResponseEntity<String>>

    /**
     * Valida un token JWT y retorna el Auth0 ID del usuario si es válido.
     */
    @GetMapping("/validate")
    fun validate(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<String>
}
