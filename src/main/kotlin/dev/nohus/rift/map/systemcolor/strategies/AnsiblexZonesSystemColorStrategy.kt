package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.MapAnsiblexZonesController.AnsiblexZone
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus

class AnsiblexZonesSystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.ansiblexZone != null
    }

    override fun getColor(system: Int): Color {
        return when (systemStatus[system]?.ansiblexZone) {
            AnsiblexZone.Zone1 -> Color(0xFF2E74DF)
            AnsiblexZone.Zone2 -> Color(0xFF70E552)
            AnsiblexZone.Zone3 -> Color(0xFFEEFF83)
            AnsiblexZone.Zone4 -> Color(0xFFDC6C08)
            AnsiblexZone.Zone5 -> Color(0xFFBC1113)
            null -> Color.Unspecified
        }
    }
}
