package dev.nohus.rift.network.zkillboardqueue

import dev.nohus.rift.killboard.KillmailConverter
import dev.nohus.rift.killboard.KillmailProcessor
import dev.nohus.rift.network.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Single
class ZkillboardObserver(
    private val zkillboardQueueApi: ZkillboardQueueApi,
    private val killmailConverter: KillmailConverter,
    private val killmailProcessor: KillmailProcessor,
) {

    private val queueId = UUID.randomUUID().toString()

    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                val result = zkillboardQueueApi.getKillmail(queueId, 5)
                when (result) {
                    is Result.Success -> {
                        val payload = result.data.payload
                        if (payload != null) {
                            val killmail = killmailConverter.convert(payload)
                            killmailProcessor.submit(killmail)
                        }
                    }
                    is Result.Failure -> {
                        logger.error { "Failed to receive killmail: ${result.cause?.message ?: "unknown error"}" }
                    }
                }
            }
        }
    }
}
