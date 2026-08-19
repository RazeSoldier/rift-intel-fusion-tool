package dev.nohus.rift.network.requests

import dev.nohus.rift.network.requests.CacheStatistics.CacheStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single
import retrofit2.Invocation

private val logger = KotlinLogging.logger {}

@Single
class CacheStatisticsLocalInterceptor(
    private val cacheStatistics: CacheStatistics,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val invocation = request.tag(Invocation::class.java)
        val endpointAnnotation = invocation?.method()?.getAnnotation(EndpointTag::class.java)
        val endpoint = endpointAnnotation?.value?.objectInstance ?: request.tag(Endpoint::class.java)

        val response = chain.proceed(request)

        if (endpoint != null) {
            val usedNetwork = response.networkResponse != null
            if (!usedNetwork) {
                cacheStatistics.addRequest(endpoint, CacheStatus.LocalCacheHit, response.code)
            }
        }

        return response
    }
}
