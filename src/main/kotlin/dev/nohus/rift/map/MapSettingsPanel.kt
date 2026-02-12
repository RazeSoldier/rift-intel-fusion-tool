package dev.nohus.rift.map

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftAutocompleteTextField
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftPill
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.*
import dev.nohus.rift.i18n.getStringSync
import dev.nohus.rift.map.DistanceMapController.DistanceMapState
import dev.nohus.rift.map.MapJumpRangeController.MapJumpRangeState
import dev.nohus.rift.map.MapLayoutRepository.Layout
import dev.nohus.rift.map.MapPlanetsController.MapPlanetsState
import dev.nohus.rift.map.MapViewModel.MapType
import dev.nohus.rift.map.MapViewModel.MapType.ClusterRegionsMap
import dev.nohus.rift.map.MapViewModel.MapType.ClusterSystemsMap
import dev.nohus.rift.map.MapViewModel.MapType.DistanceMap
import dev.nohus.rift.map.MapViewModel.MapType.RegionMap
import dev.nohus.rift.map.MapViewModel.SystemInfoTypes
import dev.nohus.rift.map.PanelState.CellColor
import dev.nohus.rift.map.PanelState.Collapsed
import dev.nohus.rift.map.PanelState.DistanceMapCenter
import dev.nohus.rift.map.PanelState.Expanded
import dev.nohus.rift.map.PanelState.Indicators
import dev.nohus.rift.map.PanelState.InfoBox
import dev.nohus.rift.map.PanelState.JumpRange
import dev.nohus.rift.map.PanelState.Planets
import dev.nohus.rift.map.PanelState.SovereigntyUpgrades
import dev.nohus.rift.map.PanelState.StarColor
import dev.nohus.rift.repositories.PlanetTypes
import dev.nohus.rift.repositories.PlanetTypes.PlanetType
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.settings.persistence.MapSystemInfoType
import dev.nohus.rift.sovupgrades.MapSovereigntyUpgradesController.MapSovereigntyUpgradesState
import dev.nohus.rift.sovupgrades.SovereigntyUpgradesRepository
import dev.nohus.rift.utils.plural
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import dev.nohus.rift.settings.persistence.MapType as SettingsMapType

enum class PanelState {
    Collapsed,
    Expanded,
    StarColor,
    CellColor,
    Indicators,
    InfoBox,
    JumpRange,
    Planets,
    SovereigntyUpgrades,
    DistanceMapCenter,
}

private val editableInfoTypes = mapOf(
    MapSystemInfoType.JumpRange to JumpRange,
    MapSystemInfoType.Planets to Planets,
    MapSystemInfoType.SovereigntyUpgrades to SovereigntyUpgrades,
)

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun MapSettingsPanel(
    hazeState: HazeState,
    mapType: MapType,
    systemInfoTypes: SystemInfoTypes,
    mapJumpRangeState: MapJumpRangeState,
    mapPlanetsState: MapPlanetsState,
    mapSovereigntyUpgradesState: MapSovereigntyUpgradesState,
    distanceMapState: DistanceMapState,
    alternativeLayouts: List<Layout>,
    onSystemColorChange: (SettingsMapType, MapSystemInfoType) -> Unit,
    onSystemColorHover: (SettingsMapType, MapSystemInfoType, Boolean) -> Unit,
    onCellColorChange: (SettingsMapType, MapSystemInfoType?) -> Unit,
    onCellColorHover: (SettingsMapType, MapSystemInfoType?, Boolean) -> Unit,
    onIndicatorChange: (SettingsMapType, MapSystemInfoType) -> Unit,
    onInfoBoxChange: (SettingsMapType, MapSystemInfoType) -> Unit,
    onJumpRangeTargetUpdate: (String) -> Unit,
    onJumpRangeDistanceUpdate: (Double) -> Unit,
    onPlanetTypesUpdate: (List<PlanetType>) -> Unit,
    onSovereigntyUpgradeTypesUpdate: (List<Type>) -> Unit,
    onLayoutSelected: (Int) -> Unit,
    onDistanceMapCenterUpdate: (String) -> Unit,
    onDistanceMapRangeUpdate: (Int) -> Unit,
) {
    val settingsMapType = when (mapType) {
        ClusterRegionsMap -> null
        is ClusterSystemsMap -> SettingsMapType.NewEden
        is RegionMap -> SettingsMapType.Region
        is DistanceMap -> SettingsMapType.Distance
    } ?: return
    Column(
        modifier = Modifier.padding(1.dp),
    ) {
        var previousPanelState: PanelState by remember { mutableStateOf(Collapsed) }
        var panelState: PanelState by remember { mutableStateOf(Collapsed) }
        ScrollbarColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
            isScrollbarConditional = true,
            hasScrollbarBackground = false,
            modifier = Modifier
                .heightIn(max = 200.dp)
                .onPointerEvent(PointerEventType.Enter) {
                    if (panelState == Collapsed) panelState = Expanded
                }
                .onPointerEvent(PointerEventType.Exit) {
                    if (panelState == Expanded) panelState = Collapsed
                }
                .hazeChild(hazeState),
        ) {
            AnimatedContent(targetState = panelState) { state ->
                when (state) {
                    Collapsed -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.expand_more_16px),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(horizontal = Spacing.small)
                                    .size(16.dp),
                            )
                        }
                    }
                    Expanded -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier
                                .padding(Spacing.medium)
                                .fillMaxWidth(),
                        ) {
                            FlowRow(
                                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                maxItemsInEachRow = 2,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.map_window_scrollbar_system),
                                        style = RiftTheme.typography.headerPrimary,
                                    )
                                    SystemColorPills(
                                        isExpanded = false,
                                        isCellColor = false,
                                        selected = systemInfoTypes.starSelected[settingsMapType],
                                        onPillClick = {
                                            panelState = StarColor
                                        },
                                        onPillEditClick = {
                                            previousPanelState = panelState
                                            panelState = editableInfoTypes[it]!!
                                        },
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.map_window_scrollbar_background),
                                        style = RiftTheme.typography.headerPrimary,
                                    )
                                    SystemColorPills(
                                        isExpanded = false,
                                        isCellColor = true,
                                        selected = systemInfoTypes.cellSelected[settingsMapType],
                                        onPillClick = {
                                            panelState = CellColor
                                        },
                                        onPillEditClick = {
                                            previousPanelState = panelState
                                            panelState = editableInfoTypes[it]!!
                                        },
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.map_window_scrollbar_indicators),
                                        style = RiftTheme.typography.headerPrimary,
                                    )
                                    val text = systemInfoTypes.indicators[settingsMapType].orEmpty().let {
                                        if (it.isEmpty()) stringResource(Res.string.map_window_none) else stringResource(Res.string.map_window_enabled_count, it.size)
                                    }
                                    RiftPill(
                                        text = text,
                                        onClick = {
                                            panelState = Indicators
                                        },
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.map_window_scrollbar_info_box),
                                        style = RiftTheme.typography.headerPrimary,
                                    )
                                    val text = systemInfoTypes.infoBox[settingsMapType].orEmpty().let {
                                        if (it.isEmpty()) stringResource(Res.string.map_window_none) else stringResource(Res.string.map_window_enabled_count, it.size)
                                    }
                                    RiftPill(
                                        text = text,
                                        onClick = {
                                            panelState = InfoBox
                                        },
                                    )
                                }
                            }
                            if (mapType is RegionMap) {
                                AlternativeLayoutsPills(
                                    alternativeLayouts = alternativeLayouts,
                                    selectedLayoutId = mapType.layoutId,
                                    onLayoutSelected = onLayoutSelected,
                                )
                            } else if (mapType is DistanceMap) {
                                DistanceMapPills(
                                    state = distanceMapState,
                                    onDistanceMapCenterClick = {
                                        panelState = DistanceMapCenter
                                    },
                                    onDistanceMapRangeClick = {
                                        panelState = DistanceMapCenter
                                    },
                                )
                            }
                        }
                    }
                    StarColor -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(Spacing.medium),
                        ) {
                            SettingsPanelTitle(
                                title = stringResource(Res.string.map_window_settings_panel_system_color),
                                onBack = { panelState = Expanded },
                            )
                            SystemColorPills(
                                isExpanded = true,
                                isCellColor = false,
                                selected = systemInfoTypes.starSelected[settingsMapType],
                                onPillClick = {
                                    onSystemColorChange(settingsMapType, it!!)
                                    panelState = Expanded
                                },
                                onPillEditClick = {
                                    previousPanelState = panelState
                                    panelState = editableInfoTypes[it]!!
                                },
                                onPillHover = { color, isHovered ->
                                    onSystemColorHover(settingsMapType, color!!, isHovered)
                                },
                            )
                        }
                    }
                    CellColor -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(Spacing.medium),
                        ) {
                            SettingsPanelTitle(
                                title = stringResource(Res.string.map_window_settings_panel_system_background_color),
                                onBack = { panelState = Expanded },
                            )
                            SystemColorPills(
                                isExpanded = true,
                                isCellColor = true,
                                selected = systemInfoTypes.cellSelected[settingsMapType],
                                onPillClick = {
                                    onCellColorChange(settingsMapType, it)
                                    panelState = Expanded
                                },
                                onPillEditClick = {
                                    previousPanelState = panelState
                                    panelState = editableInfoTypes[it]!!
                                },
                                onPillHover = { color, isHovered ->
                                    onCellColorHover(settingsMapType, color, isHovered)
                                },
                            )
                        }
                    }
                    Indicators -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(Spacing.medium),
                        ) {
                            SettingsPanelTitle(
                                title = stringResource(Res.string.map_window_settings_panel_indicators),
                                onBack = { panelState = Expanded },
                            )
                            SystemIndicatorsPills(
                                hidden = setOf(
                                    MapSystemInfoType.StarColor,
                                    MapSystemInfoType.NullSecurity,
                                    MapSystemInfoType.IntelHostiles,
                                    MapSystemInfoType.FactionWarfare,
                                    MapSystemInfoType.RatsType,
                                ),
                                getInfoTypeNames = ::getMapStarInfoTypeIndicatorName,
                                selected = systemInfoTypes.indicators[settingsMapType].orEmpty(),
                                onPillClick = { onIndicatorChange(settingsMapType, it) },
                                onPillEditClick = {
                                    previousPanelState = panelState
                                    panelState = editableInfoTypes[it]!!
                                },
                            )
                        }
                    }
                    InfoBox -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(Spacing.medium),
                        ) {
                            SettingsPanelTitle(
                                title = stringResource(Res.string.map_window_settings_panel_info_box),
                                onBack = { panelState = Expanded },
                            )
                            SystemIndicatorsPills(
                                hidden = setOf(
                                    MapSystemInfoType.StarColor,
                                    MapSystemInfoType.NullSecurity,
                                    MapSystemInfoType.IntelHostiles,
                                ),
                                getInfoTypeNames = ::getMapStarInfoTypeInfoBoxName,
                                selected = systemInfoTypes.infoBox[settingsMapType].orEmpty(),
                                onPillClick = { onInfoBoxChange(settingsMapType, it) },
                                onPillEditClick = {
                                    previousPanelState = panelState
                                    panelState = editableInfoTypes[it]!!
                                },
                            )
                        }
                    }
                    JumpRange -> {
                        JumpRangePanel(
                            mapJumpRangeState = mapJumpRangeState,
                            onBack = { panelState = previousPanelState },
                            onJumpRangeTargetUpdate = onJumpRangeTargetUpdate,
                            onJumpRangeDistanceUpdate = onJumpRangeDistanceUpdate,
                        )
                    }
                    Planets -> {
                        PlanetsPanel(
                            mapPlanetsState = mapPlanetsState,
                            onBack = { panelState = previousPanelState },
                            onPlanetTypesUpdate = onPlanetTypesUpdate,
                        )
                    }
                    SovereigntyUpgrades -> {
                        SovereigntyUpgradesPanel(
                            mapSovereigntyUpgradesState = mapSovereigntyUpgradesState,
                            onBack = { panelState = previousPanelState },
                            onSovereigntyUpgradeTypesUpdate = onSovereigntyUpgradeTypesUpdate,
                        )
                    }
                    DistanceMapCenter -> {
                        DistanceMapPanel(
                            state = distanceMapState,
                            onBack = { panelState = Expanded },
                            onDistanceMapCenterUpdate = onDistanceMapCenterUpdate,
                            onDistanceMapRangeUpdate = onDistanceMapRangeUpdate,
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp).background(RiftTheme.colors.borderGrey),
        )
    }
}

@Composable
private fun JumpRangePanel(
    mapJumpRangeState: MapJumpRangeState,
    onBack: () -> Unit,
    onJumpRangeTargetUpdate: (String) -> Unit,
    onJumpRangeDistanceUpdate: (Double) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        SettingsPanelTitle(
            title = stringResource(Res.string.map_window_jump_range),
            onBack = onBack,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 36.dp),
        ) {
            val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
            val charactersRepository: LocalCharactersRepository = remember { koin.get() }
            var targetText by remember { mutableStateOf("") }

            val suggestions by derivedStateOf {
                val possibleCharacters = charactersRepository.characters.value
                    .mapNotNull { it.info?.name }
                val possibleSystems = solarSystemsRepository.getSystems()
                    .map { it.name }
                (possibleCharacters + possibleSystems)
                    .filter { it.lowercase().startsWith(targetText.lowercase()) }
                    .filter { it.lowercase() != targetText.lowercase() }
            }

            LaunchedEffect(mapJumpRangeState.target) {
                when (mapJumpRangeState.target) {
                    is MapJumpRangeController.MapJumpRangeTarget.Character -> targetText = mapJumpRangeState.target.name
                    is MapJumpRangeController.MapJumpRangeTarget.System -> targetText = mapJumpRangeState.target.name
                    null -> {}
                }
            }

            Text(
                text = stringResource(Res.string.from),
                style = RiftTheme.typography.bodyPrimary,
                modifier = Modifier.padding(end = Spacing.small),
            )
            RiftAutocompleteTextField(
                text = targetText,
                suggestions = suggestions.take(5),
                placeholder = stringResource(Res.string.map_window_jump_range_placeholder),
                onTextChanged = {
                    targetText = it
                    onJumpRangeTargetUpdate(it)
                },
                modifier = Modifier
                    .width(150.dp),
            )
            AnimatedVisibility(targetText.isNotBlank()) {
                RequirementIcon(
                    isFulfilled = mapJumpRangeState.target != null,
                    fulfilledTooltip = when (mapJumpRangeState.target) {
                        is MapJumpRangeController.MapJumpRangeTarget.Character -> stringResource(Res.string.map_window_valid_character)
                        is MapJumpRangeController.MapJumpRangeTarget.System -> stringResource(Res.string.map_window_valid_system)
                        null -> ""
                    },
                    notFulfilledTooltip = stringResource(Res.string.map_window_no_found_system_or_character),
                )
            }
        }
        val ranges = listOf(
            stringResource(Res.string.map_window_supercap_range) to 6.0,
            stringResource(Res.string.map_window_cap_range) to 7.0,
            stringResource(Res.string.map_window_blop_range) to 8.0,
            stringResource(Res.string.map_window_other_cap_range) to 10.0,
        )
        RiftDropdownWithLabel(
            label = stringResource(Res.string.map_window_range_label),
            items = ranges,
            selectedItem = ranges.firstOrNull { it.second == mapJumpRangeState.distanceLy } ?: ranges.first(),
            onItemSelected = { onJumpRangeDistanceUpdate(it.second) },
            getItemName = { it.first },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanetsPanel(
    mapPlanetsState: MapPlanetsState,
    onBack: () -> Unit,
    onPlanetTypesUpdate: (List<PlanetType>) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        SettingsPanelTitle(
            title = stringResource(Res.string.map_window_planet_types),
            onBack = onBack,
        )
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            PlanetTypes.types.forEach { type ->
                val isSelected = type in mapPlanetsState.selectedTypes
                RiftPill(
                    text = type.name,
                    icon = type.icon,
                    isIconColor = true,
                    isSelected = isSelected,
                    onClick = {
                        val new = if (isSelected) {
                            mapPlanetsState.selectedTypes - type
                        } else {
                            mapPlanetsState.selectedTypes + type
                        }
                        onPlanetTypesUpdate(new)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SovereigntyUpgradesPanel(
    mapSovereigntyUpgradesState: MapSovereigntyUpgradesState,
    onBack: () -> Unit,
    onSovereigntyUpgradeTypesUpdate: (List<Type>) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        SettingsPanelTitle(
            title = stringResource(Res.string.map_window_sov_upgrade_types),
            onBack = onBack,
        )
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            val sovereigntyUpgradesRepository: SovereigntyUpgradesRepository = remember { koin.get() }
            sovereigntyUpgradesRepository.groupedUpgradeTypes.forEach { group ->
                val isSelected = group.any { it in mapSovereigntyUpgradesState.selectedTypes }
                val name = group.first().name.replace(Regex("\\s+\\d+$"), "")
                RiftPill(
                    text = name,
                    icon = {
                        Row {
                            for (type in group) {
                                AsyncTypeIcon(
                                    type = type,
                                    modifier = Modifier
                                        .padding(end = Spacing.small)
                                        .size(32.dp),
                                )
                            }
                        }
                    },
                    isSelected = isSelected,
                    onClick = {
                        val new = if (isSelected) {
                            mapSovereigntyUpgradesState.selectedTypes - group
                        } else {
                            mapSovereigntyUpgradesState.selectedTypes + group
                        }
                        onSovereigntyUpgradeTypesUpdate(new)
                    },
                )
            }
        }
    }
}

@Composable
private fun DistanceMapPanel(
    state: DistanceMapState,
    onBack: () -> Unit,
    onDistanceMapCenterUpdate: (String) -> Unit,
    onDistanceMapRangeUpdate: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        SettingsPanelTitle(
            title = stringResource(Res.string.map_window_distance_map),
            onBack = onBack,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 36.dp),
        ) {
            val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
            val charactersRepository: LocalCharactersRepository = remember { koin.get() }
            var targetText by remember { mutableStateOf("") }
            var isEdited by remember { mutableStateOf(false) }

            val suggestions by derivedStateOf {
                val possibleCharacters = charactersRepository.characters.value
                    .mapNotNull { it.info?.name }
                val possibleSystems = solarSystemsRepository.getSystems()
                    .map { it.name }
                (possibleCharacters + possibleSystems)
                    .filter { it.lowercase().startsWith(targetText.lowercase()) }
                    .filter { it.lowercase() != targetText.lowercase() }
            }

            LaunchedEffect(state.followingCharacterId, state.followingCharacterName, state.centerSystemId, state.centerSystemName) {
                targetText = when {
                    state.followingCharacterId != null -> state.followingCharacterName ?: state.followingCharacterId.toString()
                    else -> state.centerSystemName ?: state.centerSystemId.toString()
                }
            }

            Text(
                text = stringResource(Res.string.map_window_centered_on),
                style = RiftTheme.typography.bodyPrimary,
                modifier = Modifier.padding(end = Spacing.small),
            )
            RiftAutocompleteTextField(
                text = targetText,
                suggestions = suggestions.take(5),
                placeholder = stringResource(Res.string.map_window_jump_range_placeholder),
                onTextChanged = {
                    targetText = it
                    isEdited = true
                    onDistanceMapCenterUpdate(it)
                },
                modifier = Modifier
                    .width(150.dp),
            )
            AnimatedVisibility(targetText.isNotBlank()) {
                RequirementIcon(
                    isFulfilled = state.isEditedCenterValid || !isEdited,
                    fulfilledTooltip = when {
                        state.followingCharacterId != null -> stringResource(Res.string.map_window_valid_character)
                        else -> stringResource(Res.string.map_window_valid_system)
                    },
                    notFulfilledTooltip = stringResource(Res.string.map_window_no_found_system_or_character),
                )
            }
        }
        val ranges = List(5) {
            val range = it + 1
            pluralStringResource(Res.plurals.map_window_jump_text, range, range) to range
        }
        RiftDropdownWithLabel(
            label = stringResource(Res.string.map_window_range_label),
            items = ranges,
            selectedItem = ranges.firstOrNull { it.second == state.distance } ?: ranges.first(),
            onItemSelected = { onDistanceMapRangeUpdate(it.second) },
            getItemName = { it.first },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsPanelTitle(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.onClick { onBack() },
    ) {
        RiftImageButton(
            resource = Res.drawable.backicon,
            size = 20.dp,
            onClick = onBack,
        )
        Text(
            text = title,
            style = RiftTheme.typography.headerPrimary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlternativeLayoutsPills(
    alternativeLayouts: List<Layout>,
    selectedLayoutId: Int,
    onLayoutSelected: (Int) -> Unit,
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = stringResource(Res.string.map_window_alternative_map),
            style = RiftTheme.typography.headerPrimary,
        )
        alternativeLayouts.forEach { layout ->
            RiftPill(
                text = layout.name,
                isSelected = layout.layoutId == selectedLayoutId,
                onClick = {
                    onLayoutSelected(layout.layoutId)
                },
            )
        }
    }
}

@Composable
private fun DistanceMapPills(
    state: DistanceMapState,
    onDistanceMapCenterClick: () -> Unit,
    onDistanceMapRangeClick: () -> Unit,
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                text = stringResource(Res.string.map_window_centered_on),
                style = RiftTheme.typography.headerPrimary,
            )
            if (state.followingCharacterId != null) {
                RiftPill(
                    text = state.followingCharacterName ?: state.followingCharacterId.toString(),
                    onClick = onDistanceMapCenterClick,
                )
            } else {
                RiftPill(
                    text = state.centerSystemName ?: state.centerSystemId.toString(),
                    onClick = onDistanceMapCenterClick,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Text(
                text = stringResource(Res.string.map_window_range_label),
                style = RiftTheme.typography.headerPrimary,
            )
            RiftPill(
                text = pluralStringResource(Res.plurals.map_window_jump_text, state.distance, state.distance),
                onClick = onDistanceMapRangeClick,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SystemColorPills(
    isExpanded: Boolean,
    isCellColor: Boolean,
    selected: MapSystemInfoType?,
    onPillClick: (MapSystemInfoType?) -> Unit,
    onPillEditClick: (MapSystemInfoType?) -> Unit,
    onPillHover: (MapSystemInfoType?, Boolean) -> Unit = { _, _ -> },
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        val colorEntries = MapSystemInfoType.entries - listOf(MapSystemInfoType.Planets, MapSystemInfoType.Region, MapSystemInfoType.Constellation)
        val pills = if (isCellColor) colorEntries + null else colorEntries
        pills.filter { isExpanded || selected == it }
            .forEach { type ->
                val (text, tooltip) = getMapStarInfoTypeColorName(type)
                RiftTooltipArea(
                    text = tooltip,
                ) {
                    RiftPill(
                        text = text,
                        isSelected = isExpanded && selected == type,
                        onClick = {
                            onPillClick(type)
                        },
                        onEditClick = editableInfoTypes[type]?.let { { onPillEditClick(type) } },
                        onHoverChange = {
                            onPillHover(type, it)
                        },
                    )
                }
            }
    }
}

@Composable
private fun SystemIndicatorsPills(
    hidden: Set<MapSystemInfoType>,
    getInfoTypeNames: (color: MapSystemInfoType?) -> Pair<String, String>,
    selected: List<MapSystemInfoType>,
    onPillClick: (MapSystemInfoType) -> Unit,
    onPillEditClick: (MapSystemInfoType) -> Unit,
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier,
    ) {
        val pills = MapSystemInfoType.entries - hidden
        pills.forEach { type ->
            val (text, tooltip) = getInfoTypeNames(type)
            RiftTooltipArea(
                text = tooltip,
            ) {
                RiftPill(
                    text = text,
                    isSelected = type in selected,
                    onClick = {
                        onPillClick(type)
                    },
                    onEditClick = editableInfoTypes[type]?.let { { onPillEditClick(type) } },
                )
            }
        }
    }
}

/**
 * System coloring
 */
private fun getMapStarInfoTypeColorName(color: MapSystemInfoType?): Pair<String, String> {
    return when (color) {
        MapSystemInfoType.StarColor -> getStringSync(Res.string.map_window_actual_color) to getStringSync(Res.string.map_window_actual_color_tooltip)
        MapSystemInfoType.Security -> getStringSync(Res.string.map_window_security_status) to getStringSync(Res.string.map_window_security_status_tooltip)
        MapSystemInfoType.NullSecurity -> getStringSync(Res.string.map_window_null_sec_status) to getStringSync(Res.string.map_window_null_sec_status_tooltip)
        MapSystemInfoType.IntelHostiles -> getStringSync(Res.string.map_window_hostiles_count) to getStringSync(Res.string.map_window_hostiles_count_tooltip)
        MapSystemInfoType.Jumps -> getStringSync(Res.string.map_window_jumps) to getStringSync(Res.string.map_window_jumps_tooltip)
        MapSystemInfoType.Kills -> getStringSync(Res.string.map_window_kills) to getStringSync(Res.string.map_window_kills_tooltip)
        MapSystemInfoType.NpcKills -> getStringSync(Res.string.map_window_npc_kills) to getStringSync(Res.string.map_window_npc_kills_tooltip)
        MapSystemInfoType.Assets -> getStringSync(Res.string.map_window_assets) to getStringSync(Res.string.map_window_assets_tooltip)
        MapSystemInfoType.Incursions -> getStringSync(Res.string.map_window_incursions) to getStringSync(Res.string.map_window_incursions_tooltip)
        MapSystemInfoType.Stations -> getStringSync(Res.string.map_window_stations) to getStringSync(Res.string.map_window_stations_tooltip)
        MapSystemInfoType.FactionWarfare -> getStringSync(Res.string.map_window_faction_warfare) to getStringSync(Res.string.map_window_faction_warfare_tooltip)
        MapSystemInfoType.Sovereignty -> getStringSync(Res.string.map_window_sovereignty) to getStringSync(Res.string.map_window_sovereignty_tooltip)
        MapSystemInfoType.SovereigntyUpgrades -> getStringSync(Res.string.map_window_sov_upgrades) to getStringSync(Res.string.map_window_sov_upgrades_tooltip)
        MapSystemInfoType.MetaliminalStorms -> getStringSync(Res.string.map_window_metaliminal_storms) to getStringSync(Res.string.map_window_metaliminal_storms_tooltip)
        MapSystemInfoType.JumpRange -> getStringSync(Res.string.map_window_jump_range) to getStringSync(Res.string.map_window_jump_range_tooltip)
        MapSystemInfoType.Planets -> throw IllegalArgumentException("Not used for colors")
        MapSystemInfoType.JoveObservatories -> getStringSync(Res.string.map_window_jove_observatories) to getStringSync(Res.string.map_window_jove_observatories_tooltip)
        MapSystemInfoType.Wormholes -> getStringSync(Res.string.map_window_wormholes) to getStringSync(Res.string.map_window_wormholes_tooltip)
        MapSystemInfoType.Colonies -> getStringSync(Res.string.map_window_pi) to getStringSync(Res.string.map_window_pi_tooltip)
        MapSystemInfoType.Clones -> getStringSync(Res.string.map_window_clones) to getStringSync(Res.string.map_window_clones_tooltip)
        MapSystemInfoType.Standings -> getStringSync(Res.string.map_window_standings) to getStringSync(Res.string.map_window_standings_tooltip)
        MapSystemInfoType.RatsType -> getStringSync(Res.string.map_window_rat) to getStringSync(Res.string.map_window_rat_tooltip)
        MapSystemInfoType.AsteroidBelts -> getStringSync(Res.string.map_window_asteroid_belts) to getStringSync(Res.string.map_window_asteroid_belts_tooltip)
        MapSystemInfoType.IceFields -> getStringSync(Res.string.map_window_ice_fields) to getStringSync(Res.string.map_window_ice_fields_tooltip)
        MapSystemInfoType.Region -> throw IllegalArgumentException("Not used for colors")
        MapSystemInfoType.Constellation -> throw IllegalArgumentException("Not used for colors")
        MapSystemInfoType.IndustryIndexCopying -> getStringSync(Res.string.map_window_copying_index) to getStringSync(Res.string.map_window_copying_index_tooltip)
        MapSystemInfoType.IndustryIndexInvention -> getStringSync(Res.string.map_window_invention_index) to getStringSync(Res.string.map_window_invention_index_tooltip)
        MapSystemInfoType.IndustryIndexManufacturing -> getStringSync(Res.string.map_window_manufacturing_index) to getStringSync(Res.string.map_window_manufacturing_index_tooltip)
        MapSystemInfoType.IndustryIndexReaction -> getStringSync(Res.string.map_window_reactions_index) to getStringSync(Res.string.map_window_reactions_index_tooltip)
        MapSystemInfoType.IndustryIndexMaterialEfficiency -> getStringSync(Res.string.map_window_material_efficeinency_index) to getStringSync(Res.string.map_window_material_efficeinency_index_tooltip)
        MapSystemInfoType.IndustryIndexTimeEfficiency -> getStringSync(Res.string.map_window_time_efficiency_index) to getStringSync(Res.string.map_window_time_efficiency_index_tooltip)
        null -> getStringSync(Res.string.map_window_none) to getStringSync(Res.string.map_window_none_tooltip)
    }
}

/**
 * System indicators
 */
private fun getMapStarInfoTypeIndicatorName(color: MapSystemInfoType?): Pair<String, String> {
    return when (color) {
        MapSystemInfoType.StarColor -> "" to ""
        MapSystemInfoType.Security -> getStringSync(Res.string.map_window_indicator_security) to getStringSync(Res.string.map_window_indicator_security_tooltip)
        MapSystemInfoType.NullSecurity -> "" to ""
        MapSystemInfoType.IntelHostiles -> "" to ""
        MapSystemInfoType.Jumps -> getStringSync(Res.string.map_window_indicator_jumps) to getStringSync(Res.string.map_window_indicator_jumps_tooltip)
        MapSystemInfoType.Kills -> getStringSync(Res.string.map_window_indicator_kills) to getStringSync(Res.string.map_window_indicator_kills_tooltip)
        MapSystemInfoType.NpcKills -> getStringSync(Res.string.map_window_indicator_npc_kills) to getStringSync(Res.string.map_window_indicator_npc_kills_tooltip)
        MapSystemInfoType.Assets -> getStringSync(Res.string.map_window_indicator_assets) to getStringSync(Res.string.map_window_indicator_assets_tooltip)
        MapSystemInfoType.Incursions -> getStringSync(Res.string.map_window_indicator_incursions) to getStringSync(Res.string.map_window_indicator_incursions_tooltip)
        MapSystemInfoType.Stations -> getStringSync(Res.string.map_window_indicator_stations) to getStringSync(Res.string.map_window_indicator_stations_tooltip)
        MapSystemInfoType.FactionWarfare -> "" to ""
        MapSystemInfoType.Sovereignty -> getStringSync(Res.string.map_window_indicator_sovereignty) to getStringSync(Res.string.map_window_indicator_sovereignty_tooltip)
        MapSystemInfoType.SovereigntyUpgrades -> getStringSync(Res.string.map_window_indicator_sov_upgrades) to getStringSync(Res.string.map_window_indicator_sov_upgrades_tooltip)
        MapSystemInfoType.MetaliminalStorms -> getStringSync(Res.string.map_window_indicator_metaliminal_storms) to getStringSync(Res.string.map_window_indicator_metaliminal_storms_tooltip)
        MapSystemInfoType.JumpRange -> getStringSync(Res.string.map_window_indicator_jump_range) to getStringSync(Res.string.map_window_indicator_jump_range_tooltip)
        MapSystemInfoType.Planets -> getStringSync(Res.string.map_window_indicator_planets) to getStringSync(Res.string.map_window_indicator_planets_tooltip)
        MapSystemInfoType.JoveObservatories -> getStringSync(Res.string.map_window_indicator_jove_observatories) to getStringSync(Res.string.map_window_indicator_jove_observatories_tooltip)
        MapSystemInfoType.Wormholes -> getStringSync(Res.string.map_window_indicator_wormholes) to getStringSync(Res.string.map_window_indicator_wormholes_tooltip)
        MapSystemInfoType.Colonies -> getStringSync(Res.string.map_window_indicator_pi) to getStringSync(Res.string.map_window_indicator_pi_tooltip)
        MapSystemInfoType.Clones -> getStringSync(Res.string.map_window_indicator_clones) to getStringSync(Res.string.map_window_indicator_clones_tooltip)
        MapSystemInfoType.Standings -> getStringSync(Res.string.map_window_indicator_standings) to getStringSync(Res.string.map_window_indicator_standings_tooltip)
        MapSystemInfoType.RatsType -> "" to ""
        MapSystemInfoType.AsteroidBelts -> getStringSync(Res.string.map_window_indicator_asteroid_belts) to getStringSync(Res.string.map_window_indicator_asteroid_belts_tooltip)
        MapSystemInfoType.IceFields -> getStringSync(Res.string.map_window_indicator_ice_fields) to getStringSync(Res.string.map_window_indicator_ice_fields_tooltip)
        MapSystemInfoType.Region -> getStringSync(Res.string.map_window_indicator_region) to getStringSync(Res.string.map_window_indicator_region_tooltip)
        MapSystemInfoType.Constellation -> getStringSync(Res.string.map_window_indicator_constellation) to getStringSync(Res.string.map_window_indicator_constellation_tooltip)
        MapSystemInfoType.IndustryIndexCopying -> getStringSync(Res.string.map_window_indicator_copying_index) to getStringSync(Res.string.map_window_indicator_copying_index_tooltip)
        MapSystemInfoType.IndustryIndexInvention -> getStringSync(Res.string.map_window_indicator_invention_index) to getStringSync(Res.string.map_window_indicator_invention_index_tooltip)
        MapSystemInfoType.IndustryIndexManufacturing -> getStringSync(Res.string.map_window_indicator_manufacturing_index) to getStringSync(Res.string.map_window_indicator_manufacturing_index_tooltip)
        MapSystemInfoType.IndustryIndexReaction -> getStringSync(Res.string.map_window_indicator_reactions_index) to getStringSync(Res.string.map_window_indicator_reactions_index_tooltip)
        MapSystemInfoType.IndustryIndexMaterialEfficiency -> getStringSync(Res.string.map_window_indicator_material_efficeinency_index) to getStringSync(Res.string.map_window_indicator_material_efficeinency_index_tooltip)
        MapSystemInfoType.IndustryIndexTimeEfficiency -> getStringSync(Res.string.map_window_indicator_time_efficiency_index) to getStringSync(Res.string.map_window_indicator_time_efficiency_index_tooltip)
        null -> getStringSync(Res.string.map_window_none) to getStringSync(Res.string.map_window_none_tooltip)
    }
}

/**
 * System info box indicators
 */
private fun getMapStarInfoTypeInfoBoxName(color: MapSystemInfoType?): Pair<String, String> {
    return when (color) {
        MapSystemInfoType.StarColor -> "" to ""
        MapSystemInfoType.Security -> getStringSync(Res.string.map_window_indicator_security) to getStringSync(Res.string.map_window_indicator_security_tooltip)
        MapSystemInfoType.NullSecurity -> "" to ""
        MapSystemInfoType.IntelHostiles -> "" to ""
        MapSystemInfoType.Jumps -> getStringSync(Res.string.map_window_indicator_jumps) to getStringSync(Res.string.map_window_indicator_jumps_tooltip)
        MapSystemInfoType.Kills -> getStringSync(Res.string.map_window_indicator_kills) to getStringSync(Res.string.map_window_indicator_kills_tooltip)
        MapSystemInfoType.NpcKills -> getStringSync(Res.string.map_window_indicator_npc_kills) to getStringSync(Res.string.map_window_indicator_npc_kills_tooltip)
        MapSystemInfoType.Assets -> getStringSync(Res.string.map_window_assets) to getStringSync(Res.string.map_window_assets_tooltip)
        MapSystemInfoType.Incursions -> getStringSync(Res.string.map_window_incursions) to getStringSync(Res.string.map_window_incursions_tooltip)
        MapSystemInfoType.Stations -> getStringSync(Res.string.map_window_indicator_incursions) to getStringSync(Res.string.map_window_indicator_incursions_tooltip)
        MapSystemInfoType.FactionWarfare -> getStringSync(Res.string.map_window_info_box_faction_warfare) to getStringSync(Res.string.map_window_info_box_faction_warfare_tooltip)
        MapSystemInfoType.Sovereignty -> getStringSync(Res.string.map_window_info_box_sov) to getStringSync(Res.string.map_window_info_box_sov_tooltip)
        MapSystemInfoType.SovereigntyUpgrades -> getStringSync(Res.string.map_window_info_box_sov_upgrades) to getStringSync(Res.string.map_window_info_box_sov_upgrades_tooltip)
        MapSystemInfoType.MetaliminalStorms -> getStringSync(Res.string.map_window_info_box_metaliminal_storms) to getStringSync(Res.string.map_window_info_box_metaliminal_storms_tooltip)
        MapSystemInfoType.JumpRange -> getStringSync(Res.string.map_window_info_box_jump_range) to getStringSync(Res.string.map_window_info_box_jump_range_tooltip)
        MapSystemInfoType.Planets -> getStringSync(Res.string.map_window_info_box_planets) to getStringSync(Res.string.map_window_info_box_planets_tooltip)
        MapSystemInfoType.JoveObservatories -> getStringSync(Res.string.map_window_info_box_jove_observatories) to getStringSync(Res.string.map_window_info_box_jove_observatories_tooltip)
        MapSystemInfoType.Wormholes -> getStringSync(Res.string.map_window_info_box_wormholes) to getStringSync(Res.string.map_window_info_box_wormholes_tooltip)
        MapSystemInfoType.Colonies -> getStringSync(Res.string.map_window_info_box_pi) to getStringSync(Res.string.map_window_info_box_pi_tooltip)
        MapSystemInfoType.Clones -> getStringSync(Res.string.map_window_info_box_clones) to getStringSync(Res.string.map_window_info_box_clones_tooltip)
        MapSystemInfoType.Standings -> getStringSync(Res.string.map_window_indicator_standings) to getStringSync(Res.string.map_window_indicator_standings_tooltip)
        MapSystemInfoType.RatsType -> getStringSync(Res.string.map_window_info_box_rats) to getStringSync(Res.string.map_window_info_box_rats_tooltip)
        MapSystemInfoType.AsteroidBelts -> getStringSync(Res.string.map_window_info_box_asteroid_belts) to getStringSync(Res.string.map_window_info_box_asteroid_belts_tooltip)
        MapSystemInfoType.IceFields -> getStringSync(Res.string.map_window_info_box_ice_fields) to getStringSync(Res.string.map_window_info_box_ice_fields_tooltip)
        MapSystemInfoType.Region -> getStringSync(Res.string.map_window_indicator_region) to getStringSync(Res.string.map_window_indicator_region_tooltip)
        MapSystemInfoType.Constellation -> getStringSync(Res.string.map_window_indicator_constellation) to getStringSync(Res.string.map_window_indicator_constellation_tooltip)
        MapSystemInfoType.IndustryIndexCopying -> getStringSync(Res.string.map_window_indicator_copying_index) to getStringSync(Res.string.map_window_indicator_copying_index_tooltip)
        MapSystemInfoType.IndustryIndexInvention -> getStringSync(Res.string.map_window_indicator_invention_index) to getStringSync(Res.string.map_window_indicator_invention_index_tooltip)
        MapSystemInfoType.IndustryIndexManufacturing -> getStringSync(Res.string.map_window_indicator_manufacturing_index) to getStringSync(Res.string.map_window_indicator_manufacturing_index_tooltip)
        MapSystemInfoType.IndustryIndexReaction -> getStringSync(Res.string.map_window_indicator_reactions_index) to getStringSync(Res.string.map_window_indicator_reactions_index_tooltip)
        MapSystemInfoType.IndustryIndexMaterialEfficiency -> getStringSync(Res.string.map_window_indicator_material_efficeinency_index) to getStringSync(Res.string.map_window_indicator_material_efficeinency_index_tooltip)
        MapSystemInfoType.IndustryIndexTimeEfficiency -> getStringSync(Res.string.map_window_indicator_time_efficiency_index) to getStringSync(Res.string.map_window_indicator_time_efficiency_index_tooltip)
        null -> getStringSync(Res.string.map_window_none) to getStringSync(Res.string.map_window_none_tooltip)
    }
}
