package entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "author")
data class Author(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = true, unique = true)
    val auth0Id: String,

    @OneToMany(mappedBy = "author")
    val snippets: List<UserSnippet> = emptyList(),
) {
    constructor() : this(null, "@", "nonexistent")

    override fun toString(): String {
        return "Author(id=$id, email='$email', auth0Id='$auth0Id')"
    }
}

data class CreateUser(
    val email: String,
)

