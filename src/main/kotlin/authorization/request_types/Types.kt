package authorization.request_types

data class UserSnippet(
    val snippetId: Long,
    val role: String,
)

data class CheckRequest(
    val snippetId: Long,
    val email: String,
)
