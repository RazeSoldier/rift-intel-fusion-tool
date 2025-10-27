package dev.nohus.rift.network.interceptors

import dev.nohus.rift.BuildConfig
import dev.nohus.rift.network.requests.Originator
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.koin.core.annotation.Single

@Single
class UserAgentInterceptor : Interceptor {

    companion object {
        const val USER_AGENT_KEY = "User-Agent"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val originator = request.tag(Originator::class.java) ?: return createSyntheticFailure(request, "Missing originator tag")

        val newRequest = request.newBuilder()
            .header(USER_AGENT_KEY, getUserAgent(originator))
            .build()
        return chain.proceed(newRequest)
    }

    fun getUserAgent(originator: Originator): String {
        return "RIFT/${BuildConfig.version}-source (feature:${originator.name}, developer@riftforeve.online; discord:nohus)"
    }

    private fun createSyntheticFailure(request: okhttp3.Request, message: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(400)
            .message(message)
            .body(message.toResponseBody(null))
            .build()
    }
}
