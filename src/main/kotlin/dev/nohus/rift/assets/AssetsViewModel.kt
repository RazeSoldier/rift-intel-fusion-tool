package dev.nohus.rift.assets

import dev.nohus.rift.ViewModel
import dev.nohus.rift.assets.AssetsRepository.AssetWithLocation
import dev.nohus.rift.assets.FittingController.Fitting
import dev.nohus.rift.characters.repositories.ActiveCharacterRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.network.esi.models.CharactersIdAsset
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.GetSystemDistanceUseCase
import dev.nohus.rift.repositories.PricesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.persistence.LocationPinStatus
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.sso.scopes.ScopeGroups
import dev.nohus.rift.utils.openBrowser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

@Single
class AssetsViewModel(
    private val assetsRepository: AssetsRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val getSystemDistanceUseCase: GetSystemDistanceUseCase,
    private val characterLocationRepository: CharacterLocationRepository,
    private val activeCharacterRepository: ActiveCharacterRepository,
    private val localCharactersRepository: LocalCharactersRepository,
    private val typesRepository: TypesRepository,
    private val fittingController: FittingController,
    private val pricesRepository: PricesRepository,
    private val settings: Settings,
) : ViewModel() {

    data class AssetLocation(
        val locationId: Long,
        val locationTypeId: Int?,
        val security: Double?,
        val name: String,
        val systemId: Int?,
        val distance: Int?,
    )

    data class Asset(
        val asset: CharactersIdAsset,
        val characterId: Int,
        val type: Type?,
        val name: String?,
        val typeName: String,
        val children: List<Asset>,
        val price: Double? = null,
        val fitting: Fitting? = null,
    )

    enum class SortType {
        Distance,
        Name,
        Count,
        Price,
    }

    data class AssetTotals(
        val locations: Int,
        val items: Int,
        val price: Double,
        val volume: Double,
    )

    data class UiState(
        val assets: List<Pair<AssetLocation, List<Asset>>> = emptyList(),
        val assetTotals: AssetTotals? = null,
        val characters: List<LocalCharacter> = emptyList(),
        val filterCharacter: LocalCharacter? = null,
        val search: String = "",
        val sort: SortType = SortType.Distance,
        val pins: Map<Long, LocationPinStatus> = emptyMap(),
        val isLoading: Boolean = false,
    )

    enum class FitAction {
        Copy,
        CopyWithCargo,
        Open,
    }

    private var allAssets: List<Pair<AssetLocation, List<Asset>>> = emptyList()
    private val _state = MutableStateFlow(
        UiState(
            pins = settings.assetLocationPins,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                characterLocationRepository.locations,
                activeCharacterRepository.activeCharacter,
                assetsRepository.assets,
            ) { locations, activeCharacter, (assets, isLoading) ->
                val activeCharacterLocation = locations[activeCharacter]?.solarSystemId
                pricesRepository.refreshPrices(Originator.Assets)
                val processedAssets = getAssetsByLocation(assets, activeCharacterLocation)
                processedAssets to isLoading
            }.collect { (assets, isLoading) ->
                allAssets = assets
                updateFilteredAssets()
                _state.value = _state.value.copy(isLoading = isLoading)
            }
        }
        viewModelScope.launch {
            localCharactersRepository.characters.collect { characters ->
                _state.update { it.copy(characters = characters.filter { ScopeGroups.readAssets in it.scopes }) }
            }
        }
    }

    fun onReloadClick() {
        viewModelScope.launch {
            assetsRepository.reload()
        }
    }

    fun onCharacterSelected(character: LocalCharacter?) {
        _state.update { it.copy(filterCharacter = character) }
        updateFilteredAssets()
    }

    fun onSortSelected(sort: SortType) {
        _state.update { it.copy(sort = sort) }
        updateFilteredAssets()
    }

    fun onSearchChange(text: String) {
        _state.update { it.copy(search = text) }
        updateFilteredAssets()
    }

    fun onFitAction(fitting: Fitting, action: FitAction) {
        when (action) {
            FitAction.Copy -> Clipboard.copy(fitting.eftWithoutCargo)
            FitAction.CopyWithCargo -> Clipboard.copy(fitting.eft)
            FitAction.Open -> fittingController.getEveShipFitUri(fitting.eft)?.openBrowser()
        }
    }

    fun onPinChange(locationId: Long, pinStatus: LocationPinStatus) {
        settings.assetLocationPins += locationId to pinStatus
        _state.update { it.copy(pins = it.pins + (locationId to pinStatus)) }
        updateFilteredAssets()
    }

    private fun updateFilteredAssets() {
        val filteredAssets = getFilteredAssets()
        val totals = getAssetTotals(filteredAssets)
        _state.update { it.copy(assets = filteredAssets, assetTotals = totals) }
    }

    private fun getAssetTotals(filteredAssets: List<Pair<AssetLocation, List<Asset>>>): AssetTotals? {
        if (filteredAssets.isEmpty()) return null
        val totalLocations = filteredAssets.size
        val totalItems = filteredAssets.sumOf { (_, assets) -> assets.sumOf { asset -> asset.getTotalItems() } }
        val totalPrice = filteredAssets.sumOf { (_, assets) -> assets.sumOf { asset -> asset.getTotalPrice() } }
        val totalVolume = filteredAssets.sumOf { (_, assets) -> assets.sumOf { asset -> asset.getTotalVolume() } }
        return AssetTotals(totalLocations, totalItems, totalPrice, totalVolume)
    }

    private fun getFilteredAssets(): List<Pair<AssetLocation, List<Asset>>> {
        var filtered = allAssets
        val characterId = _state.value.filterCharacter?.characterId
        if (characterId != null) {
            filtered = filtered
                .mapNotNull { (location, assets) ->
                    val characterAssets = assets.filter { it.characterId == characterId }
                    if (characterAssets.isNotEmpty()) location to characterAssets else null
                }
        }
        val search = _state.value.search.takeIf { it.isNotBlank() }?.lowercase()
        if (search != null) {
            filtered = filtered
                .mapNotNull { (location, assets) ->
                    fun filterMatching(asset: Asset): Asset? {
                        return if (search in (asset.name?.lowercase() ?: "") || search in asset.typeName.lowercase()) {
                            asset
                        } else {
                            val matchingChildren = asset.children.mapNotNull(::filterMatching)
                            if (matchingChildren.isNotEmpty()) {
                                asset.copy(children = matchingChildren)
                            } else {
                                null
                            }
                        }
                    }
                    val matchingAssets = assets.mapNotNull(::filterMatching)
                    if (matchingAssets.isNotEmpty()) location to matchingAssets else null
                }
        }
        val pins = _state.value.pins
        val sorted = when (_state.value.sort) {
            SortType.Distance -> filtered.sortedWith(
                compareBy(
                    { pins[it.first.locationId] != LocationPinStatus.Pinned },
                    { pins[it.first.locationId] == LocationPinStatus.Hidden },
                    { it.first.systemId == null },
                    { it.first.distance ?: Int.MAX_VALUE },
                    { it.first.name },
                ),
            )
            SortType.Name -> filtered.sortedWith(
                compareBy(
                    { pins[it.first.locationId] != LocationPinStatus.Pinned },
                    { pins[it.first.locationId] == LocationPinStatus.Hidden },
                    { it.first.systemId == null },
                    { it.first.name },
                ),
            )
            SortType.Count -> filtered.sortedWith(
                compareBy(
                    { pins[it.first.locationId] != LocationPinStatus.Pinned },
                    { pins[it.first.locationId] == LocationPinStatus.Hidden },
                    { it.first.systemId == null },
                    { -it.second.size },
                ),
            )
            SortType.Price -> filtered.sortedWith(
                compareBy(
                    { pins[it.first.locationId] != LocationPinStatus.Pinned },
                    { pins[it.first.locationId] == LocationPinStatus.Hidden },
                    { it.first.systemId == null },
                    { -it.second.sumOf { it.getTotalPrice() } },
                ),
            )
        }
        return sorted
    }

    private fun getAssetsByLocation(
        assets: List<AssetWithLocation>,
        activeCharacterSolarSystem: Int?,
    ): List<Pair<AssetLocation, List<Asset>>> {
        val itemIds = assets.map { it.asset.itemId }.toSet()
        return assets
            .groupBy { it.location }
            .map { (key, value) ->
                val location = getAssetLocation(key, activeCharacterSolarSystem)
                val assetsTree = getAssetTree(value, itemIds)
                location to assetsTree
            }
    }

    private fun getAssetLocation(
        location: AssetsRepository.AssetLocation,
        activeCharacterSolarSystem: Int?,
    ): AssetLocation {
        val systemId = when (location) {
            is AssetsRepository.AssetLocation.Station -> location.systemId
            is AssetsRepository.AssetLocation.Structure -> location.systemId
            is AssetsRepository.AssetLocation.System -> location.systemId
            is AssetsRepository.AssetLocation.AssetSafety -> null
            is AssetsRepository.AssetLocation.CustomsOffice -> null
            is AssetsRepository.AssetLocation.Unknown -> null
        }
        val system = systemId?.let { solarSystemsRepository.getSystem(it) }
        val distance = if (activeCharacterSolarSystem != null && systemId != null) {
            getSystemDistanceUseCase(activeCharacterSolarSystem, systemId, withJumpBridges = true)
        } else {
            null
        }
        return when (location) {
            is AssetsRepository.AssetLocation.Station -> {
                AssetLocation(location.locationId, location.typeId, system?.security, location.name, systemId, distance)
            }

            is AssetsRepository.AssetLocation.Structure -> {
                AssetLocation(location.locationId, location.typeId, system?.security, location.name, systemId, distance)
            }

            is AssetsRepository.AssetLocation.System -> {
                AssetLocation(location.locationId, null, system?.security, "${system?.name}", systemId, distance)
            }

            is AssetsRepository.AssetLocation.AssetSafety -> {
                AssetLocation(location.locationId, null, null, "Asset Safety", null, null)
            }

            is AssetsRepository.AssetLocation.Unknown -> {
                AssetLocation(location.locationId, null, null, "Unknown", null, null)
            }

            is AssetsRepository.AssetLocation.CustomsOffice -> {
                AssetLocation(location.locationId, null, null, "Customs Office / Skyhook", null, null)
            }
        }
    }

    /**
     * Returns the asset tree from the root
     */
    private fun getAssetTree(
        assets: List<AssetWithLocation>,
        itemIds: Set<Long>,
    ): List<Asset> {
        return assets
            .filter { it.asset.locationId !in itemIds }
            .map { item -> getAsset(assets, item) }
            .sortAssets()
    }

    /**
     * Returns the asset tree from the parent ID
     */
    private fun getAssetTree(
        assets: List<AssetWithLocation>,
        parentId: Long,
    ): List<Asset> {
        return assets
            .filter { it.asset.locationId == parentId }
            .map { item -> getAsset(assets, item) }
            .sortAssets()
    }

    private fun getAsset(
        assets: List<AssetWithLocation>,
        asset: AssetWithLocation,
    ): Asset {
        val type = typesRepository.getType(asset.asset.typeId)
        return Asset(
            asset = asset.asset,
            characterId = asset.characterId,
            type = type,
            name = asset.name,
            typeName = asset.typeName,
            children = getAssetTree(assets, asset.asset.itemId),
            price = pricesRepository.getPrice(asset.asset.typeId)
                .takeIf { asset.asset.isBlueprintCopy != true },
        ).run {
            fittingController.fillFitting(this)
        }
    }

    private fun List<Asset>.sortAssets(): List<Asset> {
        return sortedWith(
            compareBy(
                { it.fitting == null },
                { it.children.isEmpty() },
                { LocationFlags.getName(it.asset.locationFlag) },
                { it.name ?: it.typeName },
            ),
        )
    }
}
