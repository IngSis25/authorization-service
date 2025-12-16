package authorization.services

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Service
class Auth0Service(
    @Value("\${auth0.domain}")
    private val domain: String,
    @Value("\${auth0.management.client-id}")
    private val clientId: String,
    @Value("\${auth0.management.client-secret}")
    private val clientSecret: String,
) {
    private val webClient =
        WebClient.builder()
            .baseUrl("https://$domain")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()

    private var accessToken: String? = null
    private var tokenExpiresAt: Long = 0

    data class Auth0User(
        val user_id: String,
        val email: String?,
        val name: String?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TokenResponse(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("token_type")
        val tokenType: String,
        @JsonProperty("expires_in")
        val expiresIn: Int,
    )

    private fun getAccessToken(): Mono<String> {
        val now = System.currentTimeMillis() / 1000
        if (accessToken != null && tokenExpiresAt > now) {
            return Mono.just(accessToken!!)
        }

        return webClient.post()
            .uri("/oauth/token")
            .bodyValue(
                mapOf(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "audience" to "https://$domain/api/v2/",
                    "grant_type" to "client_credentials",
                ),
            )
            .retrieve()
            .bodyToMono<TokenResponse>()
            .map { response ->
                accessToken = response.accessToken
                tokenExpiresAt = now + response.expiresIn - 60 // Refresh 60 seconds before expiry
                response.accessToken
            }
    }

    fun getUserByAuth0Id(auth0Id: String): Mono<Auth0User> {
        return getAccessToken()
            .flatMap { token ->
                webClient.get()
                    .uri("/api/v2/users/{id}", auth0Id)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .bodyToMono<Auth0User>()
            }
    }

    fun getUserByEmail(email: String): Mono<Auth0User?> {
        return getAccessToken()
            .flatMap { token ->
                webClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.path("/api/v2/users-by-email")
                            .queryParam("email", email)
                            .build()
                    }
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .bodyToMono<List<Auth0User>>()
                    .map { users -> users.firstOrNull() }
            }
    }

    fun getAllUsers(): Mono<List<Auth0User>> {
        return getAccessToken()
            .flatMap { token ->
                webClient.get()
                    .uri("/api/v2/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .retrieve()
                    .bodyToMono<List<Auth0User>>()
            }
    }
}
