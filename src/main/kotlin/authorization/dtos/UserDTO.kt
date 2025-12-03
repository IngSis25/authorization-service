package authorization.dtos

import authorization.entities.Author


data class UserDTO(
    val id: Long?,
    val email: String,
    val auth0Id: String,
) {
    constructor(author: Author) : this(
        id = author.id ?: 0L,
        email = author.email,
        auth0Id = author.auth0Id,
    )
}
