package routes

import dtos.UserDTO
import dtos.UserSnippetDto
import entities.Author
import entities.CreateUser
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import request_types.CheckRequest
import request_types.UserSnippet

/**
 * Interfaz que define las rutas del controlador de usuarios.
 * Esta interfaz documenta los endpoints disponibles en el servicio de autorización.
 */
interface UserControllerRoutes {
    /**
     * Crea un nuevo usuario en el sistema.
     * Requiere un token JWT válido de Auth0.
     */
    @PostMapping
    fun create(
        @RequestHeader("Authorization") token: String,
        @RequestBody createUser: CreateUser,
    ): ResponseEntity<Author>

    /**
     * Obtiene un usuario por su ID.
     */
    @GetMapping("/get/{id}")
    fun getUserById(
        @PathVariable id: Long,
    ): ResponseEntity<UserDTO>

    /**
     * Obtiene un usuario por su Auth0 ID.
     */
    @GetMapping("/auth0/{auth0Id}")
    fun getUserByAuth0Id(
        @PathVariable auth0Id: String,
    ): ResponseEntity<UserDTO>

    /**
     * Obtiene un usuario por su email.
     */
    @GetMapping("/{email}")
    fun getUserByEmail(
        @PathVariable email: String,
    ): ResponseEntity<Author>

    /**
     * Obtiene todos los usuarios del sistema.
     */
    @GetMapping
    fun getAllUsers(): ResponseEntity<List<UserDTO>>

    /**
     * Obtiene los snippets de un usuario por su Auth0 ID.
     */
    @GetMapping("/get-user-snippets/{userId}")
    fun getUserSnippets(
        @PathVariable userId: String,
    ): ResponseEntity<List<UserSnippetDto>>

    /**
     * Actualiza un usuario existente.
     */
    @PutMapping("/{email}")
    fun updateUser(
        @RequestBody author: Author,
    ): ResponseEntity<Author>

    /**
     * Agrega un snippet a un usuario con un rol específico.
     */
    @PostMapping("/add-snippet/{email}")
    fun addSnippetToUser(
        @PathVariable email: String,
        @RequestBody addSnippet: UserSnippet,
    ): ResponseEntity<String>

    /**
     * Verifica si un usuario es el owner de un snippet.
     */
    @PostMapping("/check-owner")
    fun checkIfOwner(
        @RequestBody checkRequest: CheckRequest,
    ): ResponseEntity<String>

    /**
     * Valida un token JWT y retorna el ID del usuario si es válido.
     */
    @GetMapping("/validate")
    fun validate(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<Long>

    /**
     * Obtiene los IDs de los snippets de un usuario por su ID.
     */
    @GetMapping("/snippets/{id}")
    fun getUserSnippetsId(
        @PathVariable id: Long,
    ): ResponseEntity<List<Long>>
}
