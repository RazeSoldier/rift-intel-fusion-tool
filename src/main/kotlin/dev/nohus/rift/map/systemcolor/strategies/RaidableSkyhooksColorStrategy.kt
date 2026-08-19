package dev.nohus.rift.map.systemcolor.strategies

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.map.systemcolor.SystemColorStrategy
import dev.nohus.rift.repositories.MapStatusRepository.SolarSystemStatus
import java.time.Duration
import java.time.Instant

class RaidableSkyhooksColorStrategy(
    private val systemStatus: Map<Int, SolarSystemStatus>,
) : SystemColorStrategy() {

    override fun hasData(system: Int): Boolean {
        return systemStatus[system]?.raidableSkyhooks?.takeIf { it.isNotEmpty() } != null
    }

    override fun getColor(system: Int): Color {
        val skyhooks = systemStatus[system]?.raidableSkyhooks ?: return Color.Unspecified
        val now = Instant.now()
        val vulnerableIn = skyhooks
            .filter { it.vulnerableTo > now }
            .minByOrNull { it.vulnerableFrom }
            ?.vulnerableFrom
            ?.let { Duration.between(now, it) }
            ?: return Color.Unspecified

        return when {
            vulnerableIn.isNegative -> Color(0xFFBB1116)
            vulnerableIn < Duration.ofMinutes(5) -> Color(0xFFCE440F)
            vulnerableIn < Duration.ofMinutes(15) -> Color(0xFFDC6C06)
            vulnerableIn < Duration.ofMinutes(30) -> Color(0xFFF5FF83)
            vulnerableIn < Duration.ofMinutes(45) -> Color(0xFF71E754)
            else -> Color(0xFF60DBA3)
        }
    }
}
