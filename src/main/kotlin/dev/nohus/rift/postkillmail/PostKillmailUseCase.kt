package dev.nohus.rift.postkillmail

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.network.zkillboard.PostedKillmail
import dev.nohus.rift.network.zkillboard.ZkillboardApi
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class PostKillmailUseCase(
    private val zkillboardApi: ZkillboardApi,
) {
    suspend operator fun invoke(killId: String, hash: String, delay: KillmailPostingDelay): Result<PostedKillmail> {
        logger.debug { "Posting killmail to zKillboard: $killId" }
        return zkillboardApi.postKillmail(Originator.Killmails, killId, hash, delay.value)
            .onFailure { logger.error(it) { "Could not post killmail to zKillboard" } }
            .onSuccess { posted ->
                logger.info { "Posted killmail: $posted" }
                posted.url?.let {
                    it.toURIOrNull()?.openBrowser()
                }
            }
    }
}
