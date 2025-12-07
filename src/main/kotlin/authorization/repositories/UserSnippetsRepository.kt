package authorization.repositories

import authorization.entities.UserSnippet
import org.springframework.data.jpa.repository.JpaRepository

interface UserSnippetsRepository : JpaRepository<UserSnippet, Long> {
    fun findByAuth0Id(auth0Id: String): List<UserSnippet>

    fun findByAuth0IdAndSnippetId(
        auth0Id: String,
        snippetId: Long,
    ): UserSnippet?
}
