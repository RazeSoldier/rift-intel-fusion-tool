package dev.nohus.rift.network.zkillboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecentActivity(
    @SerialName("char_id")
    val characterIds: List<Int>,
)
