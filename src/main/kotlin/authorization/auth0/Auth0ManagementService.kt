package authorization.auth0

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class Auth0ManagementService(
    private val restTemplate: RestTemplate,
    @Value("\${auth0.domain}") private val domain: String,
    @Value("\${auth0.management.client-id}") private val clientId: String,
    @Value("\${auth0.management.client-secret}") private val clientSecret: String,
    @Value("\${auth0.management.audience}") private val audience: String,
) {
    private val objectMapper = jacksonObjectMapper()

    /**
     * Pide un token de la Management API usando client_credentials.
     */
    fun getManagementToken(): String {
        val url = "https://$domain/oauth/token"

        val body =
            mapOf(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "audience" to audience,
                "grant_type" to "client_credentials",
            )

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

        val entity = HttpEntity(body, headers)

        val response = restTemplate.postForEntity(url, entity, String::class.java)

        if (!response.statusCode.is2xxSuccessful) {
            throw IllegalStateException("Error getting Auth0 management token: ${response.statusCode}")
        }

        val json = objectMapper.readTree(response.body)
        return json["access_token"].asText()
    }

    /**
     * Busca usuarios en Auth0 por fragmento de email.
     * Ej: query = "azu" → email:*azu*
     */
    fun searchUsersByEmailFragment(query: String): List<Auth0User> {
        val trimmed = query.trim()

        // 1) Si está vacío o tiene menos de 3 chars, no buscamos nada en Auth0
        if (trimmed.length < 3) {
            return emptyList()
        }

        val token = getManagementToken()

        // 2) Armamos la query: email:*<texto>*  (con 3+ chars Auth0 ya no se queja)
        val url =
            "https://$domain/api/v2/users" +
                "?q=email:*$trimmed*&search_engine=v3"

        val headers =
            HttpHeaders().apply {
                setBearerAuth(token)
            }

        val entity = HttpEntity<Void>(headers)

        val response =
            restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                object : ParameterizedTypeReference<List<Auth0User>>() {},
            )

        return response.body ?: emptyList()
    }
}

data class Auth0User(
    val user_id: String? = null,
    val email: String? = null,
)
