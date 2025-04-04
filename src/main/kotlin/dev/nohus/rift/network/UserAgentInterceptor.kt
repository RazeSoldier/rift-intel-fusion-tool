package dev.nohus.rift.network

import dev.nohus.rift.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single

@Single
class UserAgentInterceptor : Interceptor {

    companion object {
        const val USER_AGENT_KEY = "User-Agent"
        val USER_AGENT = "RIFT/${BuildConfig.version}-source (developer@riftforeve.online; discord:nohus)"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(USER_AGENT_KEY, USER_AGENT)
            .build()
        return chain.proceed(request)
    }
}
