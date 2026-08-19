package dev.nohus.rift.map

import dev.nohus.rift.repositories.RatsRepository
import dev.nohus.rift.repositories.RatsRepository.RatType.TriglavianCollective
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.StationsRepository
import dev.nohus.rift.utils.roundSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

data class AssetSafetyInfo(
    val targetSystemId: Int,
    val targetSystemName: String,
    val sourceSystemCount: Int,
)

@Single
class GetAssetSafetyInfoUseCase(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val stationsRepository: StationsRepository,
    private val ratsRepository: RatsRepository,
) {

    suspend operator fun invoke(): Map<Int, AssetSafetyInfo> = withContext(Dispatchers.Default) {
        val systems = solarSystemsRepository.getSystems(knownSpace = true)
            .filter { ratsRepository.getRats(it.id) != TriglavianCollective }
        val stationSystemIds = stationsRepository.getStations().keys
        val stationSystemsBySecurity = systems
            .filter { it.id in stationSystemIds }
            .groupBy { it.securityBand }

        val targets = systems.mapNotNull { system ->
            val targetSecurityBand = when (system.securityBand) {
                SecurityBand.High -> SecurityBand.High
                SecurityBand.Low, SecurityBand.Null -> SecurityBand.Low
            }
            val target = stationSystemsBySecurity[targetSecurityBand]
                ?.minWithOrNull(
                    compareBy<MapSolarSystem> { getDistanceSquared(system, it) }
                        .thenBy { it.id },
                )
                ?: return@mapNotNull null
            system.id to target
        }.toMap()

        val sourceSystemCountByTarget = targets.entries
            .filter { (sourceSystemId, target) -> sourceSystemId != target.id }
            .groupingBy { it.value.id }
            .eachCount()

        targets.mapValues { (systemId, target) ->
            AssetSafetyInfo(
                targetSystemId = target.id,
                targetSystemName = target.name,
                sourceSystemCount = sourceSystemCountByTarget[systemId] ?: 0,
            )
        }
    }

    private val MapSolarSystem.securityBand: SecurityBand
        get() = when {
            security.roundSecurity() >= 0.5 -> SecurityBand.High
            security.roundSecurity() > 0.0 -> SecurityBand.Low
            else -> SecurityBand.Null
        }

    private fun getDistanceSquared(from: MapSolarSystem, to: MapSolarSystem): Double {
        val x = from.x - to.x
        val y = from.y - to.y
        val z = from.z - to.z
        return x * x + y * y + z * z
    }

    private enum class SecurityBand {
        High,
        Low,
        Null,
    }
}
