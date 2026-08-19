package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.EntityColorRepository
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
class SovereigntySystemColorStrategy(
    private val entityColorRepository: EntityColorRepository,
    @InjectedParam private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.sovereignty != null
    }

    override fun getColor(system: Int): Color {
        val sovereignty = systemStatus[system]?.sovereignty ?: return Color.Unspecified
        if (sovereignty.faction != null) return entityColorRepository.getFactionColorOrNull(Originator.Map, sovereignty.faction.factionId) ?: Color.Unspecified
        if (sovereignty.alliance != null) return entityColorRepository.getAllianceColorOrNull(Originator.Map, sovereignty.alliance.allianceId) ?: Color.Unspecified
        return Color.Unspecified
    }
}
