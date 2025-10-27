package dev.nohus.rift.network.interceptors

import dev.nohus.rift.network.requests.Character
import dev.nohus.rift.network.requests.RateLimit
import dev.nohus.rift.network.requests.RateLimitGroup
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.time.delay
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.koin.core.annotation.Single
import retrofit2.Invocation
import java.time.Duration
import java.time.Instant
import kotlin.math.ln

private val logger = KotlinLogging.logger {}

@Single
class EsiRateLimitInterceptor : Interceptor {

    private val mutex = Mutex()
    private val buckets = mutableMapOf<BucketKey, Bucket>()
    private val spentTokens = mutableMapOf<BucketKey, List<SpentTokens>>()

    data class BucketKey(
        val group: RateLimitGroup,
        val character: Character?,
    )

    data class SpentTokens(
        val tokens: Int,
        val returnTimestamp: Instant,
    )

    data class Bucket(
        val limit: Limit,
        val remaining: Int,
        val retryAfter: Instant?,
    )

    data class Limit(
        val tokens: Int,
        val windowSeconds: Int,
    )

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        val request = chain.request()
        val group = getRateLimitGroup(request)
        if (group != null) {
            val character = request.tag(Character::class.java)
            val bucketKey = BucketKey(group, character)
            handleRateLimit(group, bucketKey)

            val response = chain.proceed(request)
            handleResponse(response, group, bucketKey)

            response
        } else {
            // This endpoint does not have rate limits set
            chain.proceed(request)
        }
    }

    suspend fun getBuckets(): Pair<Map<BucketKey, Bucket>, Map<BucketKey, List<SpentTokens>>> {
        return mutex.withLock {
            buckets.toMap() to spentTokens.toMap()
        }
    }

    private fun getRateLimitGroup(request: Request): RateLimitGroup? {
        val invocation = request.tag(Invocation::class.java)
        val rateLimitGroupAnnotation = invocation?.method()?.getAnnotation(RateLimit::class.java)
        return rateLimitGroupAnnotation?.value?.objectInstance
    }

    private suspend fun handleRateLimit(
        group: RateLimitGroup,
        bucketKey: BucketKey,
    ) {
        val bucket = mutex.withLock {
            buckets[bucketKey]
        }
        if (bucket == null) {
            // No bucket, there were no requests yet for this rate limit group
            return
        }
        val now = Instant.now()
        if (bucket.retryAfter != null && bucket.retryAfter.isAfter(now)) {
            logger.info { "Request in group \"${group.name}\" is being rate limited, waiting until ${bucket.retryAfter}" }
            delay(Duration.between(now, bucket.retryAfter))
        } else {
            val replenishedTokens = mutex.withLock {
                val replenished = spentTokens[bucketKey]?.takeWhile { it.returnTimestamp.isBefore(now) } ?: emptyList()
                val sum = replenished.sumOf { it.tokens }
                spentTokens[bucketKey] = spentTokens[bucketKey]?.drop(replenished.size) ?: emptyList()
                buckets[bucketKey] = bucket.copy(remaining = bucket.remaining + sum)
                sum
            }
            val tokensRemaining = bucket.remaining + replenishedTokens

            if (tokensRemaining < 100) {
                val tokensPerSecond = bucket.limit.tokens / bucket.limit.windowSeconds.toFloat()
                val secondsToRegenerateTokensBackTo100 = ((100 - tokensRemaining) / tokensPerSecond)
                val waitFactor = getWaitFactor(tokensRemaining)
                val delayMillis = (secondsToRegenerateTokensBackTo100 * waitFactor * 1000).toLong()
                logger.info { "Request in group \"${group.name}\", tokens remaining: $tokensRemaining, waiting for ${delayMillis}ms before sending" }
                delay(delayMillis)
            } else {
                // 100+ tokens remaining, no need to throttle
            }
        }
    }

    /**
     * Determines how much we want to slow down requests based on remaining tokens.
     * The function returns:
     * - For 100 tokens or more -> 0 (no wait needed)
     * - For 50 tokens -> 0.22 (22% wait needed)
     * - For 0 tokens -> 1 (100% wait needed)
     * with a gradual non-linear ramp up inbetween.
     */
    private fun getWaitFactor(tokensRemaining: Int): Double {
        return (1.5513 - (ln(tokensRemaining.toDouble() + 5) / 3.0)).coerceIn(0.0..1.0)
    }

    private suspend fun handleResponse(
        response: Response,
        group: RateLimitGroup,
        bucketKey: BucketKey,
    ) {
        val groupResponse = response.header("X-Ratelimit-Group")
        val limit = parseLimit(response.header("X-Ratelimit-Limit"))
        val remaining = response.header("X-Ratelimit-Remaining")?.toIntOrNull()
        val used = response.header("X-Ratelimit-Used")?.toIntOrNull()
        val retryAfter = parseRetryAfter(response.header("Retry-After"))

        if (limit == null) {
            return // TODO: Remove once rate limits are all live, for now missing headers are expected
            logger.error { "Invalid limit response for request in group \"${group.name}\": (${response.header("X-Ratelimit-Limit")})" }
        } else if (groupResponse != group.name) {
            logger.error { "Invalid group response for request in group \"${group.name}\": $groupResponse" }
        } else if (remaining == null) {
            logger.error { "Invalid remaining response for request in group \"${group.name}\": (${response.header("X-Ratelimit-Remaining")})" }
        } else if (used == null) {
            logger.error { "Invalid used response for request in group \"${group.name}\": (${response.header("X-Ratelimit-Used")})" }
        } else {
            val now = Instant.now()
            val bucket = Bucket(limit, remaining, retryAfter)
            val newSpentTokens = SpentTokens(used, now + Duration.ofSeconds(bucket.limit.windowSeconds.toLong()))
            mutex.withLock {
                buckets[bucketKey] = bucket
                spentTokens[bucketKey] = spentTokens.getOrDefault(bucketKey, emptyList()) + newSpentTokens
            }
        }
    }

    private fun parseLimit(limit: String?): Limit? {
        if (limit == null) return null
        val parts = limit.split("/")
        val tokens = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val durationWithUnit = parts.getOrNull(1) ?: return null
        val duration = durationWithUnit.dropLast(1).toIntOrNull() ?: return null
        val unit = durationWithUnit.lastOrNull() ?: return null
        val windowSeconds = when (unit) {
            'm' -> duration * 60
            'h' -> duration * 60 * 60
            else -> return null
        }
        return Limit(tokens, windowSeconds)
    }

    private fun parseRetryAfter(retryAfter: String?): Instant? {
        if (retryAfter == null) return null
        return Instant.now().plusSeconds(retryAfter.toLong())
    }
}
