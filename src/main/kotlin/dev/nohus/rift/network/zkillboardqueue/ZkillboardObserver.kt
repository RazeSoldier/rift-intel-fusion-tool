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
import kotlinx.coroutines.CancellationException
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
private const val MIN_REQUEST_DELAY = 1_000L
private const val FAILED_REQUEST_DELAY = 5_000L

@Single
class ZkillboardObserver(
    private val zkillboardQueueApi: ZkillboardQueueApi,
    private val esiApi: EsiApi,
    private val killmailProcessor: KillmailProcessor,
    private val settings: Settings,
) {

    private val queueId = UUID.randomUUID().toString()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"))
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
                try {
                    val startTime = clock.markNow()
                    when (val result = zkillboardQueueApi.getKillmailStream(Killmails, queueId)) {
                        is Result.Success -> {
                            handleCnKillmail(result.data)
                        }
                        is Result.Failure -> {
                            logger.error { "Failed to receive killmail from CN API: ${result.cause?.message ?: "unknown error"}" }
                            delay(FAILED_REQUEST_DELAY)
                        }
                    }
                    val duration = startTime.elapsedNow()
                    if (duration.inWholeMilliseconds < MIN_REQUEST_DELAY) {
                        delay(MIN_REQUEST_DELAY - duration.inWholeMilliseconds)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "An error occurred while processing the killmail queue" }
                    delay(FAILED_REQUEST_DELAY)
                }
            }
        }
    }

    private suspend fun handleCnKillmail(response: CnKillmailStreamResponse) {
        val cnKillmail = response.killmail
        if (cnKillmail != null) {
            val now = Instant.now()
            val age = Duration.between(cnKillmail.time, now)
            if (age <= maxAge) {
                logger.info { "Processing killmail ${cnKillmail.killId} in ${cnKillmail.system}, ${age.toSeconds()}s ago" }
                val killmail = convertCnKillmailToKillmail(cnKillmail)
                killmailProcessor.submit(killmail)
            } else {
                logger.debug { "Skipping old killmail ${cnKillmail.killId}, ${age.toSeconds()}s old" }
            }
        } else {
            logger.debug { "No new killmail available" }
        }
    }

    private fun convertCnKillmailToKillmail(cnKillmail: CnKillmail): Killmail {
        logger.info { "Converting CN killmail: victim=${cnKillmail.charName}, ship=${cnKillmail.ship}, attacker=${cnKillmail.finalCharName}" }
        return Killmail(
            killmailId = cnKillmail.killId,
            killmailTime = cnKillmail.time,
            solarSystemId = cnKillmail.systemId,
            url = "https://beta.ceve-market.org/kill/${cnKillmail.killId}/",
            victim = Victim(
                characterId = cnKillmail.charId?.toInt(),
                corporationId = cnKillmail.corpId?.toInt(),
                allianceId = cnKillmail.alliId?.toInt(),
                shipTypeId = cnKillmail.shipId,
            ),
            attackers = listOfNotNull(
                cnKillmail.finalCharId?.let { 
                    Attacker(
                        characterId = it.toInt(),
                        shipTypeId = null,
                    )
                }
            ),
            position = null,
        )
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
                        delay(FAILED_REQUEST_DELAY)
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

    private suspend fun handlePackage(payload: Package) {
        when (val killmail = getKillmail(payload)) {
            is Result.Success -> killmailProcessor.submit(killmail.data)
            is Result.Failure -> {
                logger.error { "Failed to get killmail from ESI: ${killmail.cause?.message ?: "unknown error"}" }
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
                    url = "https://beta.ceve-market.org/kill/${killmail.killmailId}/",
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
