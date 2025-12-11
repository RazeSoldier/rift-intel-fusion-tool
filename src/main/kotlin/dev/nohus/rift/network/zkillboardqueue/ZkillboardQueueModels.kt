package dev.nohus.rift.network.zkillboardqueue

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class CnKillmailStreamResponse(
    @SerialName("client_id") val clientId: String,
    @SerialName("killmail") val killmail: CnKillmail?,
)

@Serializable
data class CnKillmail(
    @SerialName("kill_id") val killId: Long,
    @SerialName("char_id") val charId: Long? = null,
    @SerialName("char_name") val charName: String? = null,
    @SerialName("ship") val ship: String,
    @SerialName("ship_id") val shipId: Int,
    @SerialName("group") val group: String,
    @SerialName("corp_id") val corpId: Long? = null,
    @SerialName("corp_name") val corpName: String? = null,
    @SerialName("alli_id") val alliId: Long? = null,
    @SerialName("alli_name") val alliName: String? = null,
    @SerialName("finalchar_id") val finalCharId: Long? = null,
    @SerialName("finalchar_name") val finalCharName: String? = null,
    @SerialName("finalcorp_id") val finalCorpId: Long? = null,
    @SerialName("finalcorp_name") val finalCorpName: String? = null,
    @SerialName("finalalli_id") val finalAlliId: Long? = null,
    @SerialName("finalalli_name") val finalAlliName: String? = null,
    @SerialName("region") val region: String,
    @SerialName("system") val system: String,
    @SerialName("region_id") val regionId: Int,
    @SerialName("system_id") val systemId: Int,
    @SerialName("security") val security: Double,
    @SerialName("time") 
    @Serializable(with = IsoDateTimeSerializer::class)
    val time: Instant,
    @SerialName("isk") val isk: Double,
    @SerialName("hash") val hash: String,
)

@Serializable
data class ZkillboardQueueResponse(
    @SerialName("package")
    val payload: Package?,
)

@Serializable
data class Package(
    @SerialName("kill_id") val killmailId: Long,
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
