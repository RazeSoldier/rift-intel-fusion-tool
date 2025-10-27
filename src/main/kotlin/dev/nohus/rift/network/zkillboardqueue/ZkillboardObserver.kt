package dev.nohus.rift.network.zkillboardqueue

import dev.nohus.rift.killboard.Attacker
import dev.nohus.rift.killboard.Killmail
import dev.nohus.rift.killboard.KillmailProcessor
import dev.nohus.rift.killboard.Victim
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.requests.Originator.Killmails
import dev.nohus.rift.network.requests.Reply
import dev.nohus.rift.repositories.Position
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.collections.map
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}
private const val MIN_REQUEST_DELAY = 500L
private const val FAILED_REQUEST_DELAY = 5_000L

@Single
class ZkillboardObserver(
    private val zkillboardQueueApi: ZkillboardQueueApi,
    private val esiApi: EsiApi,
    private val killmailProcessor: KillmailProcessor,
    private val settings: Settings,
) {

    private val scope = CoroutineScope(SupervisorJob())
    private val queueId = UUID.randomUUID().toString()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.of("UTC"))
    private var maxAge: Duration = Duration.ofMinutes(5)

    suspend fun start() = coroutineScope {
        launch {
            settings.updateFlow.map { it.intelExpireSeconds }.collect {
                maxAge = Duration.ofSeconds(it.toLong())
            }
        }

        launch {
            val clock = TimeSource.Monotonic
            while (true) {
                val startTime = clock.markNow()
                val fromDate = dateFormatter.format(Instant.now() - maxAge)
                val filter = "killmail_time>=$fromDate"
                when (val result = zkillboardQueueApi.getKillmailRedirect(Killmails, queueId, 5, filter)) {
                    is Result.Success -> {
                        handleRedirect(result.data)
                    }
                    is Result.Failure -> {
                        logger.error { "Failed to receive killmail redirect: ${result.cause?.message ?: "unknown error"}" }
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

    private suspend fun handleRedirect(reply: Reply<Unit>) {
        val location = reply.headers["Location"]
        if (location != null) {
            if ("objectID=null" !in location) {
                when (val result = zkillboardQueueApi.getKillmail(Killmails, location)) {
                    is Result.Success -> {
                        val payload = result.data.payload
                        if (payload != null) {
                            handlePackage(payload)
                        }
                    }
                    is Result.Failure -> {
                        logger.error { "Failed to receive killmail object: ${result.cause?.message ?: "unknown error"}" }
                    }
                }
            } else {
                // No new killmail
            }
        } else {
            logger.error { "No location header in killmail redirect response" }
            delay(FAILED_REQUEST_DELAY)
        }
    }

    private fun handlePackage(payload: Package) {
        scope.launch {
            when (val killmail = getKillmail(payload)) {
                is Result.Success -> killmailProcessor.submit(killmail.data)
                is Result.Failure -> {
                    logger.error { "Failed to get killmail from ESI: ${killmail.cause?.message ?: "unknown error"}" }
                }
            }
        }
    }

    suspend fun getKillmail(zkillboardPackage: Package): Result<Killmail> {
        return esiApi.getKillmailIdHash(Killmails, zkillboardPackage.killmailId, zkillboardPackage.zkb.hash)
            .map { killmail ->
                Killmail(
                    killmailId = killmail.killmailId,
                    killmailTime = killmail.killmailTime,
                    solarSystemId = killmail.solarSystemId.toInt(),
                    url = "https://zkillboard.com/kill/${killmail.killmailId}/",
                    victim = Victim(
                        characterId = killmail.victim.characterId?.toInt(),
                        corporationId = killmail.victim.corporationId?.toInt(),
                        allianceId = killmail.victim.allianceId?.toInt(),
                        shipTypeId = killmail.victim.shipTypeId.toInt(),
                    ),
                    attackers = killmail.attackers.map { attacker ->
                        Attacker(
                            characterId = attacker.characterId?.toInt(),
                            shipTypeId = attacker.shipTypeId?.toInt(),
                        )
                    },
                    position = killmail.victim.position?.let { Position(it.x, it.y, it.z) },
                )
            }
    }
}
