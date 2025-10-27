package dev.nohus.rift.network.zkillboardqueue

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ZkillboardQueueResponse(
    @SerialName("package")
    val payload: Package?,
)

@Serializable
data class Package(
    @SerialName("killID") val killmailId: Long,
    @SerialName("zkb") val zkb: Zkb,
)

@Serializable
data class Zkb(
    @SerialName("awox")
    val awox: Boolean,
    @SerialName("destroyedValue")
    val destroyedValue: Double,
    @SerialName("droppedValue")
    val droppedValue: Double,
    @SerialName("fittedValue")
    val fittedValue: Double,
    @SerialName("hash")
    val hash: String,
    @SerialName("locationID")
    val locationID: Int? = null,
    @SerialName("npc")
    val npc: Boolean,
    @SerialName("points")
    val points: Int,
    @SerialName("solo")
    val solo: Boolean,
    @SerialName("totalValue")
    val totalValue: Double,
    @SerialName("labels")
    val labels: List<String>,
    @SerialName("href")
    val url: String,
)
