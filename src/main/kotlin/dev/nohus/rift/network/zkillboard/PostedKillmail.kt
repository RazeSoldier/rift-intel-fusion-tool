package dev.nohus.rift.network.zkillboard

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostedKillmail(
    @SerialName("status")
    val status: String? = null,
    @SerialName("url")
    val url: String? = null,
)
