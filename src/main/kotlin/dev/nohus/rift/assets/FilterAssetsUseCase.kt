package dev.nohus.rift.assets

import dev.nohus.rift.assets.AssetFilterComparison.BooleanValue
import dev.nohus.rift.assets.AssetFilterComparison.GroupValue
import dev.nohus.rift.assets.AssetFilterComparison.MetaGroupValue
import dev.nohus.rift.assets.AssetFilterComparison.NumberValue
import dev.nohus.rift.assets.AssetFilterComparison.TechLevelValue
import dev.nohus.rift.assets.AssetFilterComparison.TextValue
import dev.nohus.rift.assets.AssetsRepository.AssetOwner
import dev.nohus.rift.assets.AssetsViewModel.Asset
import dev.nohus.rift.assets.AssetsViewModel.AssetLocation
import org.koin.core.annotation.Single
import kotlin.math.abs

@Single
class FilterAssetsUseCase {

    operator fun invoke(
        assets: List<Pair<AssetLocation, List<Asset>>>,
        filters: List<AssetFilterDefinition>,
    ): List<Pair<AssetLocation, List<Asset>>> {
        if (filters.isEmpty()) return assets
        return assets.mapNotNull { (location, assets) ->
            val matchingAssets = assets.mapNotNull { filterMatching(it, filters) }
            if (matchingAssets.isNotEmpty()) location to matchingAssets else null
        }
    }

    private fun filterMatching(asset: Asset, filters: List<AssetFilterDefinition>): Asset? {
        return if (filters.all { asset.matches(it) }) {
            asset
        } else {
            val matchingChildren = asset.children.mapNotNull { filterMatching(it, filters) }
            if (matchingChildren.isNotEmpty()) {
                asset.copy(children = matchingChildren)
            } else {
                null
            }
        }
    }

    private fun Asset.matches(filter: AssetFilterDefinition): Boolean {
        if (filter.subfilters.isEmpty()) return true
        return when (filter.match) {
            AssetFilterMatch.All -> filter.subfilters.all { matches(it) }
            AssetFilterMatch.Any -> filter.subfilters.any { matches(it) }
        }
    }

    private fun Asset.matches(subfilter: AssetSubfilter): Boolean {
        return when (val comparison = subfilter.comparison) {
            is BooleanValue -> getBooleanValue(subfilter.attribute) == comparison.value
            is NumberValue -> getNumberValue(subfilter.attribute)?.matches(comparison) ?: false
            is TextValue -> getTextValue(subfilter.attribute).matches(comparison)
            is GroupValue -> matches(comparison)
            is MetaGroupValue -> matches(comparison)
            is TechLevelValue -> matches(comparison)
        }
    }

    private fun Asset.getBooleanValue(attribute: AssetFilterAttribute): Boolean? {
        return when (attribute) {
            AssetFilterAttribute.Assembled -> isAssembled
            AssetFilterAttribute.BlueprintCopy -> isBlueprintCopy
            else -> null
        }
    }

    private fun Asset.getNumberValue(attribute: AssetFilterAttribute): Double? {
        return when (attribute) {
            AssetFilterAttribute.UnitPrice -> price
            AssetFilterAttribute.MetaLevel -> type.metaLevel?.toDouble()
            AssetFilterAttribute.StackSize -> quantity.toDouble()
            AssetFilterAttribute.Volume -> (type.repackagedVolume ?: type.volume).toDouble() * quantity
            else -> null
        }
    }

    private fun Asset.getTextValue(attribute: AssetFilterAttribute): String {
        return when (attribute) {
            AssetFilterAttribute.Group -> groupName.orEmpty()
            AssetFilterAttribute.Name -> name ?: type.name
            AssetFilterAttribute.Owner -> when (owner) {
                is AssetOwner.Character -> owner.character.info?.name.orEmpty()
                is AssetOwner.Corporation -> owner.corporationName
            }
            else -> ""
        }
    }

    private fun Asset.matches(comparison: GroupValue): Boolean {
        val matches = type.categoryId == comparison.categoryId && (comparison.groupId == null || type.groupId == comparison.groupId)
        return when (comparison.operator) {
            GroupFilterOperator.Is -> matches
            GroupFilterOperator.IsNot -> !matches
        }
    }

    private fun Asset.matches(comparison: MetaGroupValue): Boolean {
        val matches = type.metaGroupId == comparison.metaGroupId
        return when (comparison.operator) {
            GroupFilterOperator.Is -> matches
            GroupFilterOperator.IsNot -> !matches
        }
    }

    private fun Asset.matches(comparison: TechLevelValue): Boolean {
        val matches = type.techLevel == comparison.techLevel
        return when (comparison.operator) {
            GroupFilterOperator.Is -> matches
            GroupFilterOperator.IsNot -> !matches
        }
    }

    private fun Double.matches(comparison: NumberValue): Boolean {
        return when (comparison.operator) {
            NumberFilterOperator.LessThan -> this < comparison.value
            NumberFilterOperator.EqualTo -> abs(this - comparison.value) < 0.000001
            NumberFilterOperator.GreaterThan -> this > comparison.value
        }
    }

    private fun String.matches(comparison: TextValue): Boolean {
        val actual = lowercase()
        val expected = comparison.value.lowercase()
        return when (comparison.operator) {
            TextFilterOperator.StartsWith -> actual.startsWith(expected)
            TextFilterOperator.DoesNotStartWith -> !actual.startsWith(expected)
            TextFilterOperator.Is -> actual == expected
            TextFilterOperator.IsNot -> actual != expected
            TextFilterOperator.Contains -> expected in actual
            TextFilterOperator.DoesNotContain -> expected !in actual
        }
    }
}
