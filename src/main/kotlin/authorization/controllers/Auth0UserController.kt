package authorization.controllers

import authorization.auth0.Auth0ManagementService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth0")
class Auth0UserController(
    private val auth0ManagementService: Auth0ManagementService,
) {
    @GetMapping("/users")
    fun searchUsers(
        @RequestParam search: String,
    ): ResponseEntity<List<UserDTO>> {
        val users =
            auth0ManagementService
                .searchUsersByEmailFragment(search)
                .filter { it.email != null && it.user_id != null }
                .map { UserDTO(id = it.user_id!!, email = it.email!!) }

        return ResponseEntity.ok(users)
    }
}

data class UserDTO(
    val id: String,
    val email: String,
)
