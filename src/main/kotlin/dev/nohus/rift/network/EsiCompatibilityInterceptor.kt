package dev.nohus.rift.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.koin.core.annotation.Single

/**
 * [Documentation](https://developers.eveonline.com/docs/services/esi/overview/#versioning)
 */
@Single
class EsiCompatibilityInterceptor : Interceptor {

    companion object {
        const val COMPATIBILITY_DATE_KEY = "X-Compatibility-Date"
        const val COMPATIBILITY_DATE = "2025-07-25"
    }

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val request = chain.request().newBuilder()
            .header(COMPATIBILITY_DATE_KEY, COMPATIBILITY_DATE)
            .build()
        chain.proceed(request)
    }
}
