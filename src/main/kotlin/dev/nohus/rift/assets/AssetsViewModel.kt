package dev.nohus.rift.assets

import dev.nohus.rift.ViewModel
import dev.nohus.rift.assets.AssetsRepository.AssetBalance
import dev.nohus.rift.assets.AssetsRepository.AssetOwner
import dev.nohus.rift.assets.AssetsRepository.AssetWithLocation
import dev.nohus.rift.assets.FittingController.Fitting
import dev.nohus.rift.characters.repositories.ActiveCharacterRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.location.CharacterLocationRepository
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.GetSystemDistanceUseCase
import dev.nohus.rift.repositories.IdRanges
import dev.nohus.rift.repositories.PricesRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.persistence.LocationPinStatus
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.sso.scopes.ScopeGroups
import dev.nohus.rift.utils.openBrowser
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

private val logger = KotlinLogging.logger {}

@OptIn(FlowPreview::class)
@Factory
class AssetsViewModel(
    private val assetsRepository: AssetsRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val getSystemDistanceUseCase: GetSystemDistanceUseCase,
    private val characterLocationRepository: CharacterLocationRepository,
    private val activeCharacterRepository: ActiveCharacterRepository,
    private val localCharactersRepository: LocalCharactersRepository,
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
        val owner: AssetOwner,
        val type: Type,
        val name: String?,
        val quantity: Int,
        val itemId: Long,
        val locationFlag: String,
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
        val loadedData: Result<LoadedData>? = null,
        val filters: AssetsFilters = AssetsFilters(),
        val characters: List<LocalCharacter> = emptyList(),
        val pins: Map<Long, LocationPinStatus> = emptyMap(),
        val isLoading: Boolean = false,
        val tab: AssetsTab = AssetsTab.Owners,
    )

    data class LoadedData(
        val assets: List<AssetWithLocation>,
        val filteredAssets: List<Pair<AssetLocation, List<Asset>>>,
        val balances: List<AssetBalance>,
        val owners: List<AssetOwner>,
        val assetTotals: AssetTotals? = null,
    )

    enum class FitAction {
        Copy,
        CopyWithCargo,
        Open,
    }

    enum class AssetsTab {
        Owners,
        Assets,
    }

    private val _state = MutableStateFlow(
        UiState(
            pins = settings.assetLocationPins,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            data class UpdateParams(
                val loadedState: Result<AssetsRepository.LoadedState>?,
                val isLoading: Boolean,
                val activeCharacter: Int?,
                val characterLocations: Map<Int, CharacterLocationRepository.Location>,
                val filters: AssetsFilters,
            )

            combine(
                assetsRepository.state.map { it.loadedState },
                assetsRepository.state.map { it.isLoading },
                activeCharacterRepository.activeCharacter,
                characterLocationRepository.locations,
                _state.map { it.filters },
            ) { loadedState, isLoading, activeCharacter, characterLocations, filters ->
                UpdateParams(loadedState, isLoading, activeCharacter, characterLocations, filters)
            }
                .debounce(100)
                .collectLatest { (loadedState, isLoading, activeCharacter, characterLocations, filters) ->
                    _state.update { it.copy(isLoading = isLoading) }
                    updateAssets(loadedState, activeCharacter, characterLocations, filters)
                }
        }

        viewModelScope.launch {
            localCharactersRepository.characters.collect { characters ->
                _state.update { it.copy(characters = characters.filter { ScopeGroups.readAssets in it.scopes }) }
            }
        }
    }

    fun onVisibilityChange(visible: Boolean) {
        viewModelScope.launch {
            assetsRepository.setNeedsRealtimeUpdates(visible)
        }
    }

    fun onReloadClick() {
        viewModelScope.launch {
            assetsRepository.reload()
        }
    }

    fun onTabClick(tab: AssetsTab) {
        _state.update { it.copy(tab = tab) }
    }

    fun onFiltersUpdate(filters: AssetsFilters) {
        _state.update { it.copy(filters = filters) }
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
    }

    private suspend fun updateAssets(
        loadedState: Result<AssetsRepository.LoadedState>?,
        activeCharacter: Int?,
        characterLocations: Map<Int, CharacterLocationRepository.Location>,
        filters: AssetsFilters,
    ) {
        val loaded = loadedState ?: return
        val activeCharacterSolarSystemId = characterLocations[activeCharacter]?.solarSystemId
        val data = withContext(Dispatchers.Default) {
            loaded.map { loaded ->
                pricesRepository.refreshPrices(Originator.Assets)
                val processedAssets = getAssetsByLocation(loaded.assets, activeCharacterSolarSystemId)
                    .map { it.first to processCorporationOffices(it.second, loaded.divisionNames) }
                val filteredAssets = getFilteredAssets(processedAssets, filters)
                val totals = getAssetTotals(filteredAssets)
                LoadedData(
                    assets = loaded.assets,
                    filteredAssets = filteredAssets,
                    balances = loaded.balances,
                    owners = loaded.owners,
                    assetTotals = totals,
                )
            }
        }
        _state.update { it.copy(loadedData = data) }
    }

    private fun getAssetTotals(filteredAssets: List<Pair<AssetLocation, List<Asset>>>): AssetTotals? {
        if (filteredAssets.isEmpty()) return null
        val totalLocations = filteredAssets.size
        val totalItems = filteredAssets.sumOf { (_, assets) -> assets.sumOf { asset -> asset.getTotalItems() } }
        val totalPrice = filteredAssets.sumOf { (_, assets) -> assets.sumOf { asset -> asset.getTotalPrice() } }
        val totalVolume = filteredAssets.sumOf { (_, assets) -> assets.sumOf { asset -> asset.getTotalVolume() } }
        return AssetTotals(totalLocations, totalItems, totalPrice, totalVolume)
    }

    private fun getFilteredAssets(assets: List<Pair<AssetLocation, List<Asset>>>, filters: AssetsFilters): List<Pair<AssetLocation, List<Asset>>> {
        var filtered = assets

        if (filters.ownerTypes.isNotEmpty()) {
            filtered = filtered
                .mapNotNull { (location, assets) ->
                    val characterAssets = assets.filter {
                        filters.ownerTypes.any { ownerType ->
                            when (ownerType) {
                                OwnerType.Character -> it.owner is AssetOwner.Character
                                is OwnerType.SpecificCharacter -> it.owner is AssetOwner.Character && it.owner.character.characterId == ownerType.characterId
                                is OwnerType.SpecificCorporation -> it.owner is AssetOwner.Corporation && it.owner.corporationId == ownerType.corporationId
                            }
                        }
                    }
                    if (characterAssets.isNotEmpty()) location to characterAssets else null
                }
        }

        val search = filters.search?.lowercase()
        if (search != null) {
            filtered = filtered
                .mapNotNull { (location, assets) ->
                    fun filterMatching(asset: Asset): Asset? {
                        return if (search in (asset.name?.lowercase() ?: "") || search in asset.type.name.lowercase()) {
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
        val sorted = when (filters.sort) {
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

    private fun processCorporationOffices(assets: List<Asset>, divisionNames: Map<Int, Map<Int, String>>): List<Asset> {
        return assets.flatMap { rootAsset ->
            if (rootAsset.type.id == IdRanges.corporationOffice) {
                rootAsset.children
                    .groupBy { it.locationFlag }
                    .entries
                    .mapIndexed { index, (locationFlag, assets) ->
                        val corporationId = (rootAsset.owner as? AssetOwner.Corporation)?.corporationId
                        val divisionNames = divisionNames[corporationId] ?: emptyMap()
                        val name = when (locationFlag) {
                            "CorpSAG1" -> divisionNames[1] ?: "Corporation Hangar 1"
                            "CorpSAG2" -> divisionNames[2] ?: "Corporation Hangar 2"
                            "CorpSAG3" -> divisionNames[3] ?: "Corporation Hangar 3"
                            "CorpSAG4" -> divisionNames[4] ?: "Corporation Hangar 4"
                            "CorpSAG5" -> divisionNames[5] ?: "Corporation Hangar 5"
                            "CorpSAG6" -> divisionNames[6] ?: "Corporation Hangar 6"
                            "CorpSAG7" -> divisionNames[7] ?: "Corporation Hangar 7"
                            "CorporationGoalDeliveries" -> "Projects"
                            else -> locationFlag
                        }
                        rootAsset.copy(
                            name = name,
                            itemId = rootAsset.itemId + 10_000_000_000_000 + index,
                            locationFlag = locationFlag,
                            children = assets,
                        )
                    }
                    .sortAssets()
            } else {
                listOf(rootAsset)
            }
        }
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
        return Asset(
            owner = asset.owner,
            type = asset.type,
            name = asset.name,
            quantity = asset.asset.quantity,
            itemId = asset.asset.itemId,
            locationFlag = asset.asset.locationFlag,
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
                { LocationFlags.getName(it.locationFlag) },
                { it.name ?: it.type.name },
            ),
        )
    }
}
