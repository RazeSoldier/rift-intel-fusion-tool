package dev.nohus.rift.assets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssetFilterDefinition(
    val id: String,
    val name: String,
    val match: AssetFilterMatch = AssetFilterMatch.All,
    val subfilters: List<AssetSubfilter> = emptyList(),
)

@Serializable
enum class AssetFilterMatch {
    All,
    Any,
}

@Serializable
data class AssetSubfilter(
    val attribute: AssetFilterAttribute,
    val comparison: AssetFilterComparison,
)

@Serializable
enum class AssetFilterAttribute {
    Assembled,
    BlueprintCopy,
    UnitPrice,
    Group,
    MetaGroup,
    MetaLevel,
    Name,
    StackSize,
    Volume,
    Owner,
}

@Serializable
sealed interface AssetFilterComparison {
    @Serializable
    @SerialName("Boolean")
    data class BooleanValue(val value: Boolean) : AssetFilterComparison

    @Serializable
    @SerialName("Number")
    data class NumberValue(val operator: NumberFilterOperator, val value: Long) : AssetFilterComparison

    @Serializable
    @SerialName("Text")
    data class TextValue(val operator: TextFilterOperator, val value: String) : AssetFilterComparison

    @Serializable
    @SerialName("Group")
    data class GroupValue(val operator: GroupFilterOperator, val categoryId: Int, val groupId: Int?) : AssetFilterComparison

    @Serializable
    @SerialName("MetaGroup")
    data class MetaGroupValue(val operator: GroupFilterOperator, val metaGroupId: Int) : AssetFilterComparison
}

@Serializable
enum class NumberFilterOperator {
    LessThan,
    EqualTo,
    GreaterThan,
}

@Serializable
enum class TextFilterOperator {
    StartsWith,
    DoesNotStartWith,
    Is,
    IsNot,
    Contains,
    DoesNotContain,
}

@Serializable
enum class GroupFilterOperator {
    Is,
    IsNot,
}
