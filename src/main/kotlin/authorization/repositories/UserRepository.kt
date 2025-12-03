package authorization.repositories

import authorization.entities.Author
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface UserRepository : CrudRepository<Author, Long> {
    fun findByEmail(email: String): Author?

    @Query("SELECT a FROM Author a WHERE a.auth0Id = :auth0Id")
    fun findByAuth0Id(
        @Param("auth0Id") auth0Id: String,
    ): Author?
}
