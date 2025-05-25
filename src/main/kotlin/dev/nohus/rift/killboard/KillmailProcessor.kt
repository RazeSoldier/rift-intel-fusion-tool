package dev.nohus.rift.killboard

import dev.nohus.rift.alerts.AlertsTriggerController
import dev.nohus.rift.intel.state.IntelStateController
import dev.nohus.rift.intel.state.SystemEntity
import dev.nohus.rift.repositories.CelestialsRepository
import dev.nohus.rift.repositories.ShipTypesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.StarGatesRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.standings.StandingsRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger {}

@Single
class KillmailProcessor(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val intelStateController: IntelStateController,
    private val typeRepository: TypesRepository,
    private val shipTypesRepository: ShipTypesRepository,
    private val characterDetailsRepository: CharacterDetailsRepository,
    private val standingsRepository: StandingsRepository,
    private val celestialsRepository: CelestialsRepository,
    private val starGatesRepository: StarGatesRepository,
    private val alertsTriggerController: AlertsTriggerController,
) {

    data class ProcessedKillmail(
        val system: String,
        val ships: List<SystemEntity.Ship>,
        val victim: SystemEntity.Character?,
        val attackers: List<SystemEntity.Character>,
        val killmail: SystemEntity.Killmail,
        val celestial: SystemEntity?,
        val timestamp: Instant,
    ) {
        val entities get() = listOf(killmail) + ships + attackers + listOfNotNull(celestial)
    }

    private val seenKillmails = mutableMapOf<Int, Killboard>()
    private val mutex = Mutex()

    fun submit(message: Killmail) {
        runBlocking(Dispatchers.Default) {
            val ago = Duration.between(message.killmailTime, Instant.now())
            val system = solarSystemsRepository.getSystemName(message.solarSystemId) ?: return@runBlocking

            val deferredVictim = message.victim.characterId
                ?.let { async { characterDetailsRepository.getCharacterDetails(it) } }

            // Corporation and alliance are only loaded if there is no character, otherwise they are included with the character
            val deferredVictimCorporation = if (message.victim.characterId == null) {
                message.victim.corporationId?.let {
                    async { characterDetailsRepository.getCorporationName(it).success }
                }
            } else {
                null
            }
            val deferredVictimAlliance = if (message.victim.characterId == null) {
                message.victim.allianceId?.let {
                    async { characterDetailsRepository.getAllianceName(it).success }
                }
            } else {
                null
            }

            val deferredAttackers = message.attackers
                .mapNotNull { it.characterId }
                .map { async { characterDetailsRepository.getCharacterDetails(it) } }

            val victim = deferredVictim?.await()?.let {
                SystemEntity.Character(it.name, it.characterId, it)
            }
            val attackers = deferredAttackers.awaitAll().filterNotNull().map {
                SystemEntity.Character(it.name, it.characterId, it)
            }
            val ships = message.attackers
                .mapNotNull { attacker ->
                    val shipName = shipTypesRepository.getShipName(attacker.shipTypeId) ?: return@mapNotNull null
                    val standing = attackers.firstOrNull { it.characterId == attacker.characterId }?.details?.standingLevel ?: Standing.Neutral
                    standing to shipName
                }
                .groupBy { it.first }
                .mapValues { (standing, ships) ->
                    ships
                        .map { it.second }
                        .groupBy { it }
                        .map { (name, ships) -> SystemEntity.Ship(name, ships.size, standing = standing) }
                }
                .flatMap { it.value }
            val standingLevel = standingsRepository.getStandingLevel(message.victim.allianceId, message.victim.corporationId, message.victim.characterId)

            val killmailVictim = SystemEntity.KillmailVictim(
                characterId = message.victim.characterId,
                details = victim?.details,
                corporationId = message.victim.corporationId ?: victim?.details?.corporationId,
                corporationName = victim?.details?.corporationName ?: deferredVictimCorporation?.await()?.name,
                corporationTicker = victim?.details?.corporationTicker ?: deferredVictimCorporation?.await()?.ticker,
                allianceId = message.victim.allianceId ?: victim?.details?.allianceId,
                allianceName = victim?.details?.allianceName ?: deferredVictimAlliance?.await()?.name,
                allianceTicker = victim?.details?.allianceTicker ?: deferredVictimAlliance?.await()?.ticker,
                standing = standingLevel,
            )
            val victimShipType = message.victim.shipTypeId?.let { typeRepository.getType(it) }
            val killmail = SystemEntity.Killmail(
                url = message.url,
                ship = shipTypesRepository.getShipName(message.victim.shipTypeId),
                typeName = victimShipType?.name,
                victim = killmailVictim,
            )
            val celestial = getCelestial(message, victimShipType)

            val processedKillmail = ProcessedKillmail(
                system = system,
                ships = ships,
                victim = victim,
                attackers = attackers,
                killmail = killmail,
                celestial = celestial,
                timestamp = message.killmailTime,
            )

            mutex.withLock {
                if (message.killmailId !in seenKillmails) {
                    seenKillmails[message.killmailId] = message.killboard
                    logger.debug { "Kill from ${message.killboard}: ${killmail.ship} killed by ${ships.joinToString { it.name }} in ${processedKillmail.system}, ${ago.toSeconds()}s ago" }
                    intelStateController.submitKillmail(processedKillmail)
                    alertsTriggerController.onNewKillmail(processedKillmail)
                } else {
                    val killboard = seenKillmails[message.killmailId]
                    logger.debug { "Kill from ${message.killboard}, ignoring, already seen from $killboard" }
                }
            }
        }
    }

    private fun getCelestial(message: Killmail, victimShipType: Type?): SystemEntity? {
        message.position ?: return null
        val closestCelestial = celestialsRepository.getClosestCelestial(message.solarSystemId, message.position) ?: return null
        val shipRadius = victimShipType?.radius ?: 0f
        val distance = (closestCelestial.distance - shipRadius).coerceAtLeast(0.0)
        val distanceKm = (distance / 1000).roundToInt()
        if (distanceKm >= 1000) return null

        val stargateSystemName = starGatesRepository.getStargates(message.solarSystemId)
            .filter { it.second == closestCelestial.celestial.type.id }
            .mapNotNull { solarSystemsRepository.getSystemName(it.first) }
            .singleOrNull { it in closestCelestial.celestial.name }
        return if (stargateSystemName != null) {
            SystemEntity.Gate(stargateSystemName, isAnsiblex = false, distanceKm = distanceKm)
        } else {
            SystemEntity.Celestial(closestCelestial.celestial, distanceKm)
        }
    }
}
