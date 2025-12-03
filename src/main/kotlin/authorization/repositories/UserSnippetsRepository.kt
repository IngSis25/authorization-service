package authorization.repositories

import authorization.entities.UserSnippet
import org.springframework.data.jpa.repository.JpaRepository

interface UserSnippetsRepository : JpaRepository<UserSnippet, Long> {
    fun findByAuthorId(authorId: Long): List<UserSnippet>
}
