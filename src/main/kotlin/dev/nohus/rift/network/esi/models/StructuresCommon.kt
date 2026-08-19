package dev.nohus.rift.network.esi.models

import dev.nohus.rift.network.IsoDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class VulnerabilityWindow(
    @SerialName("start")
    @Serializable(with = IsoDateTimeSerializer::class)
    val start: Instant,
    @SerialName("end")
    @Serializable(with = IsoDateTimeSerializer::class)
    val end: Instant,
)

@Serializable
data class ReinforcementTimer(
    @SerialName("end")
    @Serializable(with = IsoDateTimeSerializer::class)
    val end: Instant,
)
