package dev.nohus.rift.sovupgrades

import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.persistence.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single
class SovereigntyUpgradesRepository(
    private val settings: Settings,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val typesRepository: TypesRepository,
) {

    private val _upgrades = MutableStateFlow(getUpgrades())
    val upgrades = _upgrades.asStateFlow()

    val upgradeGroupIds = listOf(
        4768, // Sovereignty Hub Anomaly Detection Upgrades
        4772, // Sovereignty Hub Service Infrastructure Upgrade
        4838, // Sovereignty Hub Colony Resources Management Upgrades
        4839, // Sovereignty Hub System Effect Generator Upgrades
    )

    val groupedUpgradeTypes = listOf(
        listOf(
            82499, // Minor Threat Detection Array 1,
            82500, // Minor Threat Detection Array 2,
            82492, // Minor Threat Detection Array 3,
        ),
        listOf(
            82496, // Major Threat Detection Array 1,
            82497, // Major Threat Detection Array 2,
            82498, // Major Threat Detection Array 3,
        ),
        listOf(
            87948, // Exploration Detector 1,
            87953, // Exploration Detector 2,
            87954, // Exploration Detector 3,
        ),
        listOf(
            82579, // Tritanium Prospecting Array 1,
            82592, // Tritanium Prospecting Array 2,
            87702, // Tritanium Prospecting Array 3,
        ),
        listOf(
            82589, // Pyerite Prospecting Array 1,
            82588, // Pyerite Prospecting Array 2,
            87704, // Pyerite Prospecting Array 3,
        ),
        listOf(
            82591, // Mexallon Prospecting Array 1,
            82590, // Mexallon Prospecting Array 2,
            87705, // Mexallon Prospecting Array 3,
        ),
        listOf(
            82581, // Isogen Prospecting Array 1,
            82580, // Isogen Prospecting Array 2,
            87706, // Isogen Prospecting Array 3,
        ),
        listOf(
            82583, // Nocxium Prospecting Array 1,
            82582, // Nocxium Prospecting Array 2,
            87707, // Nocxium Prospecting Array 3,
        ),
        listOf(
            82585, // Zydrine Prospecting Array 1,
            82584, // Zydrine Prospecting Array 2,
            87708, // Zydrine Prospecting Array 3,
        ),
        listOf(
            82587, // Megacyte Prospecting Array 1,
            82586, // Megacyte Prospecting Array 2,
            87709, // Megacyte Prospecting Array 3,
        ),
        listOf(
            87950, // Electric Stability Generator,
        ),
        listOf(
            87951, // Exotic Stability Generator,
        ),
        listOf(
            87815, // Gamma Stability Generator,
        ),
        listOf(
            87949, // Plasma Stability Generator,
        ),
        listOf(
            81621, // Advanced Logistics Network,
        ),
        listOf(
            81615, // Cynosural Navigation,
        ),
        listOf(
            81619, // Cynosural Suppression,
        ),
        listOf(
            81623, // Supercapital Construction Facilities,
        ),
        listOf(
            87703, // Power Monitoring Division 1,
            88221, // Power Monitoring Division 2,
            88227, // Power Monitoring Division 3,
        ),
        listOf(
            87710, // Workforce Mecha-Tooling 1,
            88228, // Workforce Mecha-Tooling 2,
            88229, // Workforce Mecha-Tooling 3,
        ),
    ).mapNotNull { group ->
        group.mapNotNull { name -> typesRepository.getType(name) }.takeIf { it.isNotEmpty() }
    }

    private fun getUpgrades(): Map<MapSolarSystem, List<Type>> {
        return settings.sovereigntyUpgrades
            .mapNotNull { (systemName, typeIds) ->
                val system = solarSystemsRepository.getSystem(systemName) ?: return@mapNotNull null
                val types = typeIds.mapNotNull { typeId -> typesRepository.getType(typeId) }
                if (types.isEmpty()) return@mapNotNull null
                system to types
            }
            .toMap()
    }

    fun setUpgrades(upgrades: Map<MapSolarSystem, List<Type>>) {
        _upgrades.value = upgrades
        settings.sovereigntyUpgrades = upgrades
            .map { (system, types) ->
                system.name to types.map { it.id }
            }.toMap()
    }

    fun setUpgrades(system: MapSolarSystem, upgrades: List<Type>) {
        setUpgrades(this.upgrades.value + (system to upgrades))
    }
}
