package dev.nohus.rift.network.esi.models

import dev.nohus.rift.network.IsoDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Skyhooks(
    @SerialName("skyhooks")
    val skyhooks: List<Skyhook>,
)

@Serializable
data class Skyhook(
    @SerialName("id")
    val id: Long,
    @SerialName("planet_id")
    val planetId: Long,
)

@Serializable
data class SkyhooksId(
    @SerialName("id")
    val id: Long,
    @SerialName("planet_id")
    val planetId: Int,
    @SerialName("effective_workforce")
    val effectiveWorkforce: Int? = null,
    @SerialName("reagents")
    val reagents: List<SkyhookReagent>? = null,
    @SerialName("reinforcement_timer")
    val reinforcementTimer: ReinforcementTimer? = null,
    @SerialName("enabled")
    val isEnabled: Boolean? = null,
    @SerialName("state")
    val state: SkyhookState,
    @SerialName("theft_vulnerability")
    val theftVulnerability: VulnerabilityWindow? = null,
)

@Serializable
enum class SkyhookState {
    @SerialName("Unspecified")
    Unspecified,
    @SerialName("ShieldVulnerable")
    ShieldVulnerable,
    @SerialName("ArmorReinforced")
    ArmorReinforced,
    @SerialName("ArmorVulnerable")
    ArmorVulnerable,
    @SerialName("HullReinforced")
    HullReinforced,
    @SerialName("HullVulnerable")
    HullVulnerable,
}

@Serializable
data class SkyhookReagent(
    @SerialName("last_cycle")
    @Serializable(with = IsoDateTimeSerializer::class)
    val lastCycle: Instant,
    @SerialName("secured_stock")
    val securedStock: Int,
    @SerialName("unsecured_stock")
    val unsecuredStock: Int,
    @SerialName("type_id")
    val typeId: Int,
)

@Serializable
data class SkyhooksRaidable(
    @SerialName("skyhooks")
    val skyhooks: List<RaidableSkyhook>,
)

@Serializable
data class RaidableSkyhook(
    @SerialName("planet_id")
    val planetId: Int,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
    @SerialName("theft_vulnerability")
    val theftVulnerability: VulnerabilityWindow,
)
