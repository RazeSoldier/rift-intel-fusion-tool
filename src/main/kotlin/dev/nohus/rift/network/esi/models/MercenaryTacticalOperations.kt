package dev.nohus.rift.network.esi.models

import dev.nohus.rift.network.IsoDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class MercenaryTacticalOperations(
    @SerialName("operations")
    val operations: List<MercenaryTacticalOperation>,
)

@Serializable
data class MercenaryTacticalOperation(
    @SerialName("id")
    val id: String,
    @SerialName("mercenary_den_id")
    val mercenaryDenId: Long,
)

@Serializable
data class MercenaryTacticalOperationsId(
    @SerialName("id")
    val id: String,
    @SerialName("mercenary_den_id")
    val mercenaryDenId: Long,
    @SerialName("dungeon_type_id")
    val dungeonTypeId: Int,
    @SerialName("expires")
    @Serializable(with = IsoDateTimeSerializer::class)
    val expires: Instant,
    @SerialName("state")
    val state: MercenaryTacticalOperationState,
)

@Serializable
enum class MercenaryTacticalOperationState {
    @SerialName("Unspecified")
    Unspecified,

    @SerialName("Available")
    Available,

    @SerialName("Started")
    Started,

    @SerialName("Completed")
    Completed,

    @SerialName("Expired")
    Expired,

    @SerialName("Removed")
    Removed,
}
