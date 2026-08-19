package dev.nohus.rift.network.zkillboard

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

@Single
class ZkillboardRateLimitInterceptor : Interceptor {

    // Rate limit is 600 request per minute
    private val maxQuota = 500 // A bit lower than the real max
    private val quotaPerSecond = 8 // Regenerate quota a bit slower than allowed
    private var quota = 100 // Assume at start-up most of the quota is used up

    private var lastReplenish = Instant.now()
    private val mutex = Mutex()

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        mutex.withLock {
            replenish()
            if (quota < 10) {
                return@runBlocking createEmptyResponse(chain.request())
            }
            quota--
        }

        val request = chain.request()
        val response = chain.proceed(request)

        response
    }

    private fun replenish() {
        val now = Instant.now()
        val secondsPassed = Duration.between(lastReplenish, now).seconds.toInt()
        if (secondsPassed > 0) {
            quota = (quota + (secondsPassed * quotaPerSecond)).coerceAtMost(maxQuota)
            lastReplenish = now
        }
    }

    private fun createEmptyResponse(request: Request): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody(null))
            .build()
    }
}
