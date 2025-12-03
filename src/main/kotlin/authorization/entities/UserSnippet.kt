package authorization.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "user_snippets")
data class UserSnippet(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    @ManyToOne
    val author: Author,
    @Column(nullable = false)
    val snippetId: Long,
    @Column(nullable = false)
    val role: String,
) {
    constructor() : this(0, Author(), 0, "nonexistent")

    override fun toString(): String {
        return "UserSnippet(snippetId=$snippetId, user=${author.email}, role='$role')"
    }
}
