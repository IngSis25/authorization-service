package services

import dtos.UserSnippetDto
import entities.Author
import entities.UserSnippet
import errors.UserNotFoundException
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import repositories.UserRepository
import repositories.UserSnippetsRepository

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userSnippetsRepository: UserSnippetsRepository,
) {
    fun getByEmail(email: String): Author? {
        val user =
            userRepository.findByEmail(email)
                ?: throw UserNotFoundException("User not found when trying to get by email")
        return user
    }

    fun getById(id: Long): Author {
        return userRepository.findById(id)
            .orElseThrow { UserNotFoundException("User not found when trying to get by id") }
    }

    fun getByAuthId(auth0Id: String): Author? {
        return userRepository.findByAuth0Id(auth0Id)
            ?: throw UserNotFoundException("User not found when trying to get by auth0Id")
    }

    fun createUser(
        email: String,
        auth0Id: String,
    ): Author {
        val author = Author(email = email, auth0Id = auth0Id)
        return userRepository.save(author)
    }

    fun getSnippetsOfUser(id: String): List<UserSnippetDto> {
        try {
            val user = getByAuthId(id)
            return userSnippetsRepository.findByAuthorId(user?.id!!)
                .map { UserSnippetDto(it.snippetId, it.role) }
        } catch (e: Exception) {
            throw UserNotFoundException("User not found when trying to get snippets")
        }
    }

    fun getAllUsers(): List<Author> {
        return userRepository.findAll().toList()
    }

    fun updateUser(author: Author): Author? {
        val userOptional = userRepository.findById(author.id!!)
        if (userOptional.isEmpty) {
            throw UserNotFoundException("User not found when trying to update it")
        }
        val updatedUser = userOptional.get().copy(email = author.email, auth0Id = author.auth0Id)
        userRepository.save(updatedUser)
        return updatedUser
    }

    fun addSnippetToUser(
        email: String,
        snippetId: Long,
        role: String,
    ): ResponseEntity<String> {
        val user =
            getByEmail(email)
                ?: throw UserNotFoundException("User not found when trying to add a snippet to it")
        val snippets = UserSnippet(author = user, snippetId = snippetId, role = role)

        if (userSnippetRelationExists(user, snippetId)) {
            return ResponseEntity.ok("User already has this snippet.")
        }

        try {
            userSnippetsRepository.save(snippets)
            return ResponseEntity.ok("Snippet added to user")
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body("Error adding snippet to user")
        }
    }

    private fun userSnippetRelationExists(
        author: Author,
        snippetId: Long,
    ): Boolean {
        userSnippetsRepository.findByAuthorId(author.id!!).forEach {
            if (it.snippetId == snippetId) {
                return true
            }
        }
        return false
    }

    fun checkIfOwner(
        snippetId: Long,
        email: String,
    ): ResponseEntity<String> {
        val user =
            userRepository.findByEmail(email)
                ?: throw UserNotFoundException("User not found when trying to check if it is the owner of a snippet")

        userSnippetsRepository.findByAuthorId(user.id!!).forEach {
            if (it.snippetId == snippetId) {
                return if (it.role == "Owner") {
                    ResponseEntity.ok("User is the owner of the snippet")
                } else {
                    ResponseEntity.badRequest().body("User is not the owner of the snippet")
                }
            }
        }
        return ResponseEntity.badRequest().body("Snippet of id provided doesn't exist")
    }

    fun getSnippetsId(id: Long): ResponseEntity<List<Long>> {
        val user =
            userRepository.findById(id)
                .orElseThrow { UserNotFoundException("User not found when trying to get snippets") }
        val snippetsId = userSnippetsRepository.findByAuthorId(user.id!!).map { it.id }
        return ResponseEntity.ok(snippetsId)
    }
}
