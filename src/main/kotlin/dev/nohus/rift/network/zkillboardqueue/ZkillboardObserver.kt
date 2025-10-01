package dev.nohus.rift.network.zkillboardqueue

import dev.nohus.rift.killboard.KillmailConverter
import dev.nohus.rift.killboard.KillmailProcessor
import dev.nohus.rift.network.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.UUID
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}
private const val MIN_REQUEST_DELAY = 500L
private const val FAILED_REQUEST_DELAY = 5_000L

@Single
class ZkillboardObserver(
    private val zkillboardQueueApi: ZkillboardQueueApi,
    private val killmailConverter: KillmailConverter,
    private val killmailProcessor: KillmailProcessor,
) {

    private val queueId = UUID.randomUUID().toString()

    suspend fun start() = coroutineScope {
        launch {
            val clock = TimeSource.Monotonic
            while (true) {
                val startTime = clock.markNow()
                when (val result = zkillboardQueueApi.getKillmail(queueId, 5)) {
                    is Result.Success -> {
                        val payload = result.data.payload
                        if (payload != null) {
                            val killmail = killmailConverter.convert(payload)
                            killmailProcessor.submit(killmail)
                        }
                    }
                    is Result.Failure -> {
                        logger.error { "Failed to receive killmail: ${result.cause?.message ?: "unknown error"}" }
                        delay(FAILED_REQUEST_DELAY)
                    }
                }
                val duration = startTime.elapsedNow()
                if (duration.inWholeMilliseconds < MIN_REQUEST_DELAY) {
                    delay(MIN_REQUEST_DELAY - duration.inWholeMilliseconds)
                }
            }
        }
    }
}
