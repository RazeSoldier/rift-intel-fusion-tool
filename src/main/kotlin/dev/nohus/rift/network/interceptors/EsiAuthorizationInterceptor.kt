package dev.nohus.rift.network.interceptors

import dev.nohus.rift.network.requests.Character
import dev.nohus.rift.network.requests.Scope
import dev.nohus.rift.sso.authentication.SsoAuthenticator
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single
import retrofit2.Invocation

@Single
class EsiAuthorizationInterceptor(
    private val ssoAuthenticator: SsoAuthenticator,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val request = chain.request()

        val character = request.tag(Character::class.java)
        val invocation = request.tag(Invocation::class.java)
        val scopeAnnotation = invocation?.method()?.getAnnotation(Scope::class.java)
        val scope = scopeAnnotation?.value?.objectInstance

        val newRequest = if (character != null) {
            val accessToken = ssoAuthenticator.getValidEveAccessToken(character.id, scope)
            request.newBuilder()
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        } else {
            request
        }

        chain.proceed(newRequest)
    }
}
