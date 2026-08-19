package dev.nohus.rift.network.esi.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MercenaryDens(
    @SerialName("mercenary_dens")
    val mercenaryDens: List<MercenaryDen>,
)

@Serializable
data class MercenaryDen(
    @SerialName("id")
    val id: Long,
    @SerialName("planet_id")
    val planetId: Long,
)

@Serializable
data class MercenaryDensId(
    @SerialName("id")
    val id: Long,
    @SerialName("skyhook")
    val skyhook: MercenaryDenSkyhook,
    @SerialName("evolution")
    val evolution: MercenaryDenEvolution,
    @SerialName("infomorphs")
    val infomorphs: MercenaryDenInfomorphs,
    @SerialName("reinforcement_timer")
    val reinforcementTimer: ReinforcementTimer? = null,
    @SerialName("state")
    val state: MercenaryDenState,
    @SerialName("type_id")
    val typeId: Long,
)

@Serializable
data class MercenaryDenSkyhook(
    @SerialName("corporation_id")
    val corporationId: Int,
    @SerialName("id")
    val id: Long,
    @SerialName("planet_id")
    val planetId: Int,
)

@Serializable
data class MercenaryDenEvolution(
    @SerialName("anarchy")
    val anarchy: MercenaryDenAnarchy,
    @SerialName("development")
    val development: MercenaryDenDevelopment,
)

@Serializable
data class MercenaryDenAnarchy(
    @SerialName("amount")
    val amount: Int,
    @SerialName("level")
    val level: MercenaryDenAnarchyLevel,
)

@Serializable
enum class MercenaryDenAnarchyLevel {
    @SerialName("Unspecified")
    Unspecified,

    @SerialName("Level0")
    Level0,

    @SerialName("Level1")
    Level1,

    @SerialName("Level2")
    Level2,

    @SerialName("Level3")
    Level3,

    @SerialName("Level4")
    Level4,
}

@Serializable
data class MercenaryDenDevelopment(
    @SerialName("amount")
    val amount: Int,
    @SerialName("level")
    val level: MercenaryDenDevelopmentLevel,
)

@Serializable
enum class MercenaryDenDevelopmentLevel {
    @SerialName("Unspecified")
    Unspecified,

    @SerialName("Level0")
    Level0,

    @SerialName("Level1")
    Level1,

    @SerialName("Level2")
    Level2,

    @SerialName("Level3")
    Level3,

    @SerialName("Level4")
    Level4,
}

@Serializable
data class MercenaryDenInfomorphs(
    @SerialName("amount")
    val amount: Int,
)

@Serializable
enum class MercenaryDenState {
    @SerialName("Unspecified")
    Unspecified,

    @SerialName("Running")
    Running,

    @SerialName("Paused")
    Paused,

    @SerialName("Disabled")
    Disabled,
}
