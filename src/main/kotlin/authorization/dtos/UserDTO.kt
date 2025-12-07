package authorization.dtos

import authorization.services.Auth0Service

data class UserDTO(
    val id: String?,
    val email: String?,
    val auth0Id: String,
    val name: String?,
) {
    constructor(auth0User: Auth0Service.Auth0User) : this(
        id = auth0User.user_id,
        email = auth0User.email,
        auth0Id = auth0User.user_id,
        name = auth0User.name,
    )
}
