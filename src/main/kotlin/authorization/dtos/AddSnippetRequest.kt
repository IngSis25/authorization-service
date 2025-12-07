package authorization.dtos

data class AddSnippetRequest(
    val email: String,
    val role: String,
)
