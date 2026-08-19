package dev.nohus.rift.network.esi.models

import dev.nohus.rift.network.IsoDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class SovereigntyHubs(
    @SerialName("sovereignty_hubs")
    val sovereigntyHubs: List<SovereigntyHub>,
)

@Serializable
data class SovereigntyHub(
    @SerialName("id")
    val id: Long,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
)

@Serializable
data class SovereigntyHubsId(
    @SerialName("id")
    val id: Long,
    @SerialName("reagent_bay")
    val reagentBay: SovereigntyHubReagentBay,
    @SerialName("resources")
    val resources: SovereigntyHubResources,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
    @SerialName("upgrades")
    val upgrades: List<SovereigntyHubUpgrade>,
    @SerialName("workforce_transport")
    val workforceTransport: SovereigntyHubWorkforceTransport,
    @SerialName("vulnerability_window")
    val vulnerabilityWindow: VulnerabilityWindow?,
    @SerialName("fuel_access_list_id")
    val fuelAccessListId: Long? = null, // TODO: Use
)

@Serializable
data class SovereigntyHubReagentBay(
    @SerialName("last_updated")
    @Serializable(with = IsoDateTimeSerializer::class)
    val lastUpdated: Instant,
    @SerialName("reagents")
    val reagents: List<SovereigntyHubReagent>,
)

@Serializable
data class SovereigntyHubReagent(
    @SerialName("amount")
    val amount: Int,
    @SerialName("burning_per_hour")
    val burningPerHour: Int,
    @SerialName("type_id")
    val typeId: Int,
)

@Serializable
data class SovereigntyHubResources(
    @SerialName("power")
    val power: SovereigntyHubPower,
    @SerialName("workforce")
    val workforce: SovereigntyHubWorkforce,
)

@Serializable
data class SovereigntyHubPower(
    @SerialName("allocated")
    val allocated: Long,
    @SerialName("available")
    val available: Long,
)

@Serializable
data class SovereigntyHubWorkforce(
    @SerialName("allocated")
    val allocated: Long,
    @SerialName("available")
    val available: Long,
)

@Serializable
data class SovereigntyHubUpgrade(
    @SerialName("type_id")
    val typeId: Int,
    @SerialName("power_state")
    val powerState: SovereigntyHubUpgradePowerState,
)

@Serializable
enum class SovereigntyHubUpgradePowerState {
    @SerialName("Unspecified")
    Unspecified,

    @SerialName("Online")
    Online,

    @SerialName("Offline")
    Offline,

    @SerialName("Low")
    Low,

    @SerialName("Pending")
    Pending,
}

@Serializable
data class SovereigntyHubWorkforceTransport(
    @SerialName("configuration")
    val configuration: SovereigntyHubWorkforceTransportConfiguration,
    @SerialName("state")
    val state: SovereigntyHubWorkforceTransportState,
)

@Serializable
data class SovereigntyHubWorkforceTransportConfiguration(
    @SerialName("import")
    val import: SovereigntyHubWorkforceTransportConfigurationImport? = null,
    @SerialName("export")
    val export: SovereigntyHubWorkforceTransportConfigurationExport? = null,
    @SerialName("transit")
    val transit: Boolean? = null,
)

@Serializable
data class SovereigntyHubWorkforceTransportConfigurationImport(
    @SerialName("sources")
    val sources: List<SovereigntyHubWorkforceTransportConfigurationImportSource>,
)

@Serializable
data class SovereigntyHubWorkforceTransportConfigurationImportSource(
    @SerialName("solar_system_id")
    val solarSystemId: Int,
)

@Serializable
data class SovereigntyHubWorkforceTransportConfigurationExport(
    @SerialName("amount")
    val amount: Int,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
)

@Serializable
data class SovereigntyHubWorkforceTransportState(
    @SerialName("import")
    val import: SovereigntyHubWorkforceTransportStateImport? = null,
    @SerialName("export")
    val export: SovereigntyHubWorkforceTransportStateExport? = null,
    @SerialName("transit")
    val transit: Boolean? = null,
)

@Serializable
data class SovereigntyHubWorkforceTransportStateImport(
    @SerialName("sources")
    val sources: List<SovereigntyHubWorkforceTransportStateImportSource>,
)

@Serializable
data class SovereigntyHubWorkforceTransportStateImportSource(
    @SerialName("amount")
    val amount: Int,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
)

@Serializable
data class SovereigntyHubWorkforceTransportStateExport(
    @SerialName("amount")
    val amount: Int,
    @SerialName("solar_system_id")
    val solarSystemId: Int,
)
