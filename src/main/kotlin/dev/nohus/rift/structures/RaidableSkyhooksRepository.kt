package dev.nohus.rift.structures

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.PlanetsRepository
import dev.nohus.rift.repositories.PlanetsRepository.Planet
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Single
class RaidableSkyhooksRepository(
    private val esiApi: EsiApi,
    private val planetsRepository: PlanetsRepository,
) {

    suspend fun getRaidableSkyhooks(originator: Originator): Result<List<RaidableSkyhook>> {
        return esiApi.getSkyhooksRaidable(originator).map {
            it.skyhooks.mapNotNull { skyhook ->
                val planet = planetsRepository.getPlanetById(skyhook.planetId)
                if (planet == null) {
                    logger.error { "Raidable Skyhook at nonexistent planet" }
                    return@mapNotNull null
                }
                RaidableSkyhook(
                    solarSystemId = skyhook.solarSystemId,
                    planet = planet,
                    vulnerableFrom = skyhook.theftVulnerability.start,
                    vulnerableTo = skyhook.theftVulnerability.end,
                )
            }
        }
    }

    data class RaidableSkyhook(
        val solarSystemId: Int,
        val planet: Planet,
        val vulnerableFrom: Instant,
        val vulnerableTo: Instant,
    )
}
