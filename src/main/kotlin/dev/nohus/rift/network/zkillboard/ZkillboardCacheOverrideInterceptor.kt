package dev.nohus.rift.network.zkillboard

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single
import retrofit2.Invocation

@Single
class ZkillboardCacheOverrideInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val invocation = request.tag(Invocation::class.java)
        val endpointAnnotation = invocation?.method()?.getAnnotation(EndpointTag::class.java)
        val endpoint = endpointAnnotation?.value?.objectInstance

        val response = chain.proceed(request)

        /**
         * Zkillboard API prevents responses from being cached locally.
         * This overrides it to cache for 1 hour.
         */
        if (endpoint is Endpoint.ZkillboardCharacterStats) {
            return response.newBuilder()
                .header("Cache-Control", "private, max-age=3600")
                .build()
        }

        return response
    }
}
