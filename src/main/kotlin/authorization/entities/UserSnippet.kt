package authorization.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user_snippet")
data class UserSnippet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "auth0_id", nullable = false)
    val auth0Id: String,
    @Column(name = "snippet_id", nullable = false)
    val snippetId: Long,
    @Column(name = "role", nullable = false)
    val role: String,
) {
    constructor() : this(0, "nonexistent", 0, "nonexistent")

    override fun toString(): String {
        return "UserSnippet(snippetId=$snippetId, auth0Id=$auth0Id, role='$role')"
    }
}
