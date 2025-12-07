package authorization.services

import authorization.dtos.UserSnippetDto
import authorization.entities.UserSnippet
import authorization.errors.UserNotFoundException
import authorization.repositories.UserSnippetsRepository
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserService(
    private val auth0Service: Auth0Service,
    private val userSnippetsRepository: UserSnippetsRepository,
) {
    fun getByEmail(email: String): Mono<Auth0Service.Auth0User> {
        return auth0Service.getUserByEmail(email)
            .switchIfEmpty(Mono.error(UserNotFoundException("User not found when trying to get by email")))
            .flatMap { user ->
                if (user != null) {
                    Mono.just(user)
                } else {
                    Mono.error(UserNotFoundException("User not found when trying to get by email"))
                }
            }
    }

    fun getByAuthId(auth0Id: String): Mono<Auth0Service.Auth0User> {
        return auth0Service.getUserByAuth0Id(auth0Id)
            .onErrorMap { e ->
                if (e is UserNotFoundException) {
                    e
                } else {
                    UserNotFoundException("User not found when trying to get by auth0Id")
                }
            }
    }

    fun getSnippetsOfUser(auth0Id: String): List<UserSnippetDto> {
        return try {
            userSnippetsRepository.findByAuth0Id(auth0Id)
                .map { UserSnippetDto(it.snippetId, it.role) }
        } catch (e: Exception) {
            throw UserNotFoundException("User not found when trying to get snippets")
        }
    }

    fun getAllUsers(): Mono<List<Auth0Service.Auth0User>> {
        return auth0Service.getAllUsers()
    }

    /**
     * Nuevo contrato: recibe auth0Id directamente (sacado del token en el controller).
     * No consulta Auth0 otra vez para esto.
     */
    fun addSnippetToUser(
        auth0Id: String,
        snippetId: Long,
        role: String,
    ): Mono<ResponseEntity<String>> {
        return Mono.fromCallable {
            try {
                val existingRelation = userSnippetsRepository.findByAuth0IdAndSnippetId(auth0Id, snippetId)
                if (existingRelation != null) {
                    ResponseEntity.ok("User already has this snippet.")
                } else {
                    val userSnippet = UserSnippet(auth0Id = auth0Id, snippetId = snippetId, role = role)
                    userSnippetsRepository.save(userSnippet)
                    ResponseEntity.ok("Snippet added to user")
                }
            } catch (e: Exception) {
                println("Error saving user snippet: ${e.message}")
                ResponseEntity.badRequest().body("Error adding snippet to user: ${e.message}")
            }
        }
    }

    /**
     * Nuevo contrato: recibe auth0Id directamente (del token).
     */
    fun checkIfOwner(
        snippetId: Long,
        auth0Id: String,
    ): Mono<ResponseEntity<String>> {
        return Mono.fromCallable {
            val userSnippets = userSnippetsRepository.findByAuth0Id(auth0Id)
            val snippet = userSnippets.find { it.snippetId == snippetId }

            if (snippet != null) {
                if (snippet.role == "Owner") {
                    ResponseEntity.ok("User is the owner of the snippet")
                } else {
                    ResponseEntity.badRequest().body("User is not the owner of the snippet")
                }
            } else {
                ResponseEntity.badRequest().body("Snippet of id provided doesn't exist")
            }
        }.onErrorResume { e: Throwable ->
            if (e is UserNotFoundException) {
                Mono.error(e)
            } else {
                Mono.just(ResponseEntity.badRequest().body("Error checking ownership: ${e.message}"))
            }
        }
    }
}
