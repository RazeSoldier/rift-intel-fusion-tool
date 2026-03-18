package dev.nohus.rift.network.interceptors

import dev.nohus.rift.BuildConfig
import dev.nohus.rift.network.esi.EsiCache
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.utils.OperatingSystem
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.koin.core.annotation.Single

@Single
class UserAgentInterceptor(
    operatingSystem: OperatingSystem,
    private val esiCache: EsiCache,
) : Interceptor {

    companion object {
        const val USER_AGENT_KEY = "User-Agent"
    }
    private val riftFragment = "RIFT/${BuildConfig.version}-source"
    private val osFragment = when (operatingSystem) {
        OperatingSystem.Linux -> "Linux"
        OperatingSystem.Windows -> "Windows"
        OperatingSystem.MacOs -> "macOS"
    }
    private val contactFragment = "developer@riftforeve.online; discord:nohus"
    var isEsiDebugInfoEnabled = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originator = request.tag(Originator::class.java) ?: return createSyntheticFailure(request, "Missing originator tag")
        val userAgent = if (request.url.host == "esi.evetech.net") {
            getEsiUserAgent(originator)
        } else {
            getUserAgent(originator)
        }

        val newRequest = request.newBuilder()
            .header(USER_AGENT_KEY, userAgent)
            .build()
        return chain.proceed(newRequest)
    }

    fun getUserAgent(originator: Originator): String {
        return "$riftFragment (feature:${originator.name}; $contactFragment)"
    }

    private fun getEsiUserAgent(originator: Originator): String {
        val debug = if (isEsiDebugInfoEnabled) " ${esiCache.getDebugInfo()}" else ""
        return "$riftFragment (feature:${originator.name}; $osFragment; $contactFragment) $debug"
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
