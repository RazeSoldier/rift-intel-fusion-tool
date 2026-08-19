package dev.nohus.rift.map

import dev.nohus.rift.repositories.IdRanges
import dev.nohus.rift.repositories.METERS_IN_LIGHT_YEAR
import dev.nohus.rift.repositories.RatsRepository
import dev.nohus.rift.repositories.RatsRepository.RatType.TriglavianCollective
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.distanceTo
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.roundSecurity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Single

@Single
class MapAnsiblexZonesController(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val ratsRepository: RatsRepository,
    private val settings: Settings,
) {

    data class MapAnsiblexZonesState(
        val capitalSystem: CapitalSystem? = null,
        val systemZones: Map<Int, AnsiblexZone> = emptyMap(),
    )

    data class CapitalSystem(
        val id: Int,
        val name: String,
    )

    enum class AnsiblexZone(val displayName: String) {
        Zone1("Zone 1"),
        Zone2("Zone 2"),
        Zone3("Zone 3"),
        Zone4("Zone 4"),
        Zone5("Zone 5"),
    }

    private val _state = MutableStateFlow(MapAnsiblexZonesState())
    val state = _state.asStateFlow()

    init {
        loadSettings()
    }

    fun onCapitalSystemUpdate(name: String) {
        val system = solarSystemsRepository.getSystem(name)
        val capitalSystem = system?.let { CapitalSystem(id = it.id, name = it.name) }
        _state.update { it.copy(capitalSystem = capitalSystem) }
        settings.ansiblexCapitalSystemId = capitalSystem?.id
        calculateSystemZones()
    }

    private fun loadSettings() {
        val capitalSystem = settings.ansiblexCapitalSystemId
            ?.let(solarSystemsRepository::getSystem)
            ?.let { CapitalSystem(id = it.id, name = it.name) }
        _state.update { it.copy(capitalSystem = capitalSystem) }
        calculateSystemZones()
    }

    private fun calculateSystemZones() {
        val capitalSystemId = _state.value.capitalSystem?.id ?: run {
            _state.update { it.copy(systemZones = emptyMap()) }
            return
        }
        val capitalSystem = solarSystemsRepository.getSystem(capitalSystemId) ?: run {
            _state.update { it.copy(systemZones = emptyMap()) }
            return
        }
        val systemZones = solarSystemsRepository
            .getSovSystems()
            .filter { isSovSystemValidAnsiblexTarget(it) }
            .associate { system ->
                val distanceLy = capitalSystem.distanceTo(system) / METERS_IN_LIGHT_YEAR
                val zone = when {
                    distanceLy <= 5.0 -> AnsiblexZone.Zone1
                    distanceLy <= 10.0 -> AnsiblexZone.Zone2
                    distanceLy <= 15.0 -> AnsiblexZone.Zone3
                    distanceLy <= 20.0 -> AnsiblexZone.Zone4
                    else -> AnsiblexZone.Zone5
                }
                system.id to zone
            }
        _state.update { it.copy(systemZones = systemZones) }
    }

    private fun isSovSystemValidAnsiblexTarget(system: MapSolarSystem): Boolean {
        if (ratsRepository.getRats(system.id) == TriglavianCollective) return false
        if (IdRanges.isJoveRegion(system.regionId)) return false
        return true
    }
}
