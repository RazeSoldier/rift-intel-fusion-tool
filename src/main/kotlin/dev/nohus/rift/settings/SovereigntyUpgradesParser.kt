package dev.nohus.rift.settings

import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.sovupgrades.SovereigntyUpgradesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single
class SovereigntyUpgradesParser(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val typesRepository: TypesRepository,
    private val sovereigntyUpgradesRepository: SovereigntyUpgradesRepository,
) {

    suspend fun parse(text: String): Map<MapSolarSystem, List<Type>> = withContext(Dispatchers.Default) {
        val allSystems = solarSystemsRepository.getSovSystems().map { it.name }
        buildList {
            for (line in text.lines()) {
                val systemName = allSystems.firstOrNull { it in line } ?: continue
                val types = typesRepository.findTypesInText(line)
                    .filter { it.groupId in sovereigntyUpgradesRepository.upgradeGroupIds }
                    .toList()
                if (types.isEmpty()) continue
                val system = solarSystemsRepository.getSystem(systemName) ?: continue
                add(system to types)
            }
        }
            .groupBy { it.first }
            .map { (system, list) ->
                system to list.flatMap { it.second }
            }
            .toMap()
    }
}
