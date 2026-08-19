package dev.nohus.rift.network.zkillboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZkillCharacterStats(
    @SerialName("dangerRatio")
    val dangerRatio: Int? = null,
)
