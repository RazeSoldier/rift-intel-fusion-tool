package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.PercentageSystemColorStrategy
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus

class AssetSafetySystemColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.assetSafety?.sourceSystemCount?.let { it > 0 } == true
    }

    override fun getColor(system: Int): Color {
        val sourceSystemCount = systemStatus[system]?.assetSafety?.sourceSystemCount ?: 0
        return when (sourceSystemCount) {
            1 -> Color(0xFF2E74DF)
            2 -> Color(0xFF5CDCA6)
            3 -> Color(0xFF70E552)
            in 4..10 -> Color(0xFFEEFF83)
            in 11..49 -> Color(0xFFDC6C08)
            in 50..99 -> Color(0xFFCE4611)
            else -> Color(0xFFBC1113)
        }
    }
}
