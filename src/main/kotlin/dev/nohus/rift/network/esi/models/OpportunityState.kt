package dev.nohus.rift.network.esi.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OpportunityState {
    @SerialName("Unspecified")
    Unspecified,

    /**
     * No serial name as this state doesn't exist in the schema of Corporation Projects and Freelance Jobs.
     * This state is created from Mercenary Tactical Operations.
     */
    Available,

    @SerialName("Active")
    Active,

    @SerialName("Closed")
    Closed,

    @SerialName("Completed")
    Completed,

    @SerialName("Expired")
    Expired,

    @SerialName("Deleted")
    Deleted,
}
