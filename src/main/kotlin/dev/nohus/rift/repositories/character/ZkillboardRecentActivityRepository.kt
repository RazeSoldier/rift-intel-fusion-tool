package dev.nohus.rift.repositories.character

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.network.zkillboard.RecentActivity
import dev.nohus.rift.network.zkillboard.ZkillboardApi
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitCancellation
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

@Single
class ZkillboardRecentActivityRepository(
    private val zkillboardApi: ZkillboardApi,
) {

    var activeCharacterIds: Set<Int>? = null
        private set

    suspend fun start() {
        logger.info { "Zkillboard recent activity tracking is disabled for CN server" }
        // CN server doesn't have the recentactivity API, keep all characters as potentially active
        activeCharacterIds = null
        // Just wait indefinitely without making API calls
        awaitCancellation()
    }

    private suspend fun getRecentActivity(originator: Originator): RecentActivity? {
        return when (val response = zkillboardApi.getRecentActivity(originator)) {
            is Result.Success -> {
                response.data
            }
            is Result.Failure -> {
                logger.error { "Could not get recent activity from zKillboard: ${response.cause?.message}" }
                null
            }
        }
    }
}
