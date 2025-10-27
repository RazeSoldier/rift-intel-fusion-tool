package dev.nohus.rift.network

import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.esi.EsiErrorException
import dev.nohus.rift.network.esi.EsiErrorResponse
import dev.nohus.rift.sso.authentication.EveSsoRepository
import dev.nohus.rift.sso.authentication.NoAuthenticationException
import dev.nohus.rift.sso.authentication.SsoAuthenticator
import dev.nohus.rift.sso.authentication.SsoException
import dev.nohus.rift.sso.scopes.EsiScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.Headers
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

private val logger = KotlinLogging.logger {}

/**
 * A deserialized response body and response headers
 */
data class Reply<T>(
    val body: T,
    val headers: Headers,
)

interface RequestExecutor {
    suspend fun <R : Any> execute(request: suspend () -> R): Result<R>
    suspend fun <R : Any> executeEveAuthorized(characterId: Int, scope: EsiScope?, request: suspend (authentication: String) -> R): Result<R>
    suspend fun <R : Any> executeEveAuthorizedWithHeaders(characterId: Int, scope: EsiScope?, request: suspend (authorization: String) -> Response<R>): Result<Reply<R>>
}

class RequestExecutorImpl(
    private val ssoAuthenticator: SsoAuthenticator,
    private val eveSsoRepository: EveSsoRepository,
    private val json: Json,
) : RequestExecutor {

    override suspend fun <R : Any> execute(
        request: suspend () -> R,
    ): Result<R> {
        return try {
            Success(withContext(Dispatchers.IO) { request() })
        } catch (e: Exception) {
            handleError(e)
        }
    }

    override suspend fun <R : Any> executeEveAuthorized(
        characterId: Int,
        scope: EsiScope?,
        request: suspend (authorization: String) -> R,
    ): Result<R> {
        return try {
            val accessToken = ssoAuthenticator.getValidEveAccessToken(characterId, scope)
            Success(withContext(Dispatchers.IO) { request("Bearer $accessToken") })
        } catch (e: Exception) {
            handleError(e, characterId)
        }
    }

    override suspend fun <R : Any> executeEveAuthorizedWithHeaders(
        characterId: Int,
        scope: EsiScope?,
        request: suspend (authorization: String) -> Response<R>,
    ): Result<Reply<R>> {
        return try {
            val accessToken = ssoAuthenticator.getValidEveAccessToken(characterId, scope)
            val response = withContext(Dispatchers.IO) { request("Bearer $accessToken") }
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Success(Reply(body, response.headers()))
                } else {
                    handleError(SerializationException("Response body was null"))
                }
            } else {
                handleError(HttpException(response))
            }
        } catch (e: Exception) {
            handleError(e, characterId)
        }
    }

    private fun handleError(e: Exception, characterId: Int? = null): Failure {
        if (e is SsoException) {
            logger.error { "Could not execute request due to SSO failure: $e" }
        } else if (e is NoAuthenticationException) {
            if (e.scope != null) {
                logger.error { "Could not execute request because the character ${e.characterId} does not allow scope \"${e.scope.id}\"" }
            } else {
                logger.error { "Could not execute request because the character ${e.characterId} is not authenticated" }
            }
        } else if (e is HttpException) {
            val body = e.response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                try {
                    val errorResponse: EsiErrorResponse = json.decodeFromString(body)
                    if ("token is not valid" in errorResponse.error) {
                        if (characterId != null) {
                            logger.error { "Character $characterId has an invalid token, removing" }
                            eveSsoRepository.removeAuthentication(characterId)
                        }
                    } else if ("Character has been deleted" in errorResponse.error) {
                        if (characterId != null) {
                            logger.error { "Character $characterId has been deleted from the game, removing" }
                            eveSsoRepository.removeAuthentication(characterId)
                        }
                    } else if ("Forbidden" == errorResponse.error) {
                        logger.debug { "Forbidden API response" }
                    } else if ("unroutable" == errorResponse.error) {
                        logger.error { "ESI request could not be routed to Tranquility, the game is probably down" }
                    } else if ("Timeout contacting tranquility" == errorResponse.error) {
                        logger.error { "ESI request timed out contacting Tranquility, the game is probably down" }
                    } else if (e.code() == 404) {
                        // Valid response
                    } else {
                        val text = buildString {
                            append(errorResponse.error)
                            if (errorResponse.ssoStatus != null) {
                                append(" (SSO status: ${errorResponse.ssoStatus})")
                            }
                        }
                        logger.error { "ESI error response: $text" }
                    }
                    return Failure(EsiErrorException(errorResponse, e.code()))
                } catch (_: SerializationException) {
                    logger.error { "API HTTP error: ${e.code()} (with unknown body) \"$body\"" }
                }
            } else {
                logger.error { "API HTTP error: ${e.code()}" }
            }
        } else if (e is IOException) {
            logger.error { "Could not execute request: $e" }
        } else if (e is SerializationException) {
            logger.error(e) { "Unexpected API response" }
        } else if (e is CancellationException) {
            // Normal behavior, operation cancelled
        } else {
            logger.error(e) { "Unknown API Error" }
        }
        return Failure(e)
    }
}
