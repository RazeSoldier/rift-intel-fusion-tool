package dev.nohus.rift.sso.authentication

import dev.nohus.rift.sso.SsoAuthority
import dev.nohus.rift.sso.authentication.Authentication.EveAuthentication
import dev.nohus.rift.sso.scopes.EsiScope
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Factory
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Factory
class SsoAuthenticator(
    private val ssoClient: SsoClient,
    private val eveSsoRepository: EveSsoRepository,
) {

    /**
     * Starts the SSO flow, redirecting the user to the SSO login page.
     * Returns once the authentication flow has finished, or failed
     */
    suspend fun authenticate(authority: SsoAuthority, scopes: List<String>) {
        val authentication = ssoClient.authenticate(authority, scopes)
        when (authority) {
            SsoAuthority.Eve -> eveSsoRepository.addAuthentication(authentication as EveAuthentication)
        }
        logger.info { "SSO authentication successful ($authority)" }
    }

    /**
     * Cancels an in progress SSO flow, if any
     */
    fun cancel() {
        ssoClient.cancel()
    }

    /**
     * Retrieves an access token, refreshing it first if needed, or null if there isn't one
     *
     * @param characterId Character ID
     * @param scope ESI scope the token has to be valid for, or null if no scopes are required. If the present
     * access token isn't valid for that scope, this method will throw.
     * @throws NoAuthenticationException When no valid token is available
     */
    suspend fun getValidEveAccessToken(characterId: Int, scope: EsiScope?): String {
        val authentication = eveSsoRepository.getAuthentication(characterId) ?: throw NoAuthenticationException(characterId, null)
        if (scope != null && scope.id !in authentication.scopes) {
            throw NoAuthenticationException(characterId, scope)
        }
        return if (authentication.expiration.isBefore(Instant.now())) {
            val newAuthentication = ssoClient.refreshToken(SsoAuthority.Eve, authentication) as EveAuthentication
            logger.debug { "Eve SSO access token refreshed" }
            eveSsoRepository.addAuthentication(newAuthentication)
            newAuthentication.accessToken
        } else {
            authentication.accessToken
        }
    }
}
