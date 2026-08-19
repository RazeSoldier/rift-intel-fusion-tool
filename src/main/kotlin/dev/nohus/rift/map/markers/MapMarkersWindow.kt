package dev.nohus.rift.map.markers

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nohus.rift.alerts.creategroup.CreateGroupDialog
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ExpandChevron
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftAutocompleteTextField
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckbox
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.allDrawableResources
import dev.nohus.rift.generated.resources.delete
import dev.nohus.rift.generated.resources.editplanicon
import dev.nohus.rift.generated.resources.flag_background
import dev.nohus.rift.generated.resources.menu_pinned
import dev.nohus.rift.generated.resources.toggle_off_18
import dev.nohus.rift.generated.resources.toggle_on_18
import dev.nohus.rift.generated.resources.window_locations
import dev.nohus.rift.map.markers.MapMarkersViewModel.UiState
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource
import java.util.UUID

@Composable
fun MapMarkersWindow(
    inputModel: MapMarkersInputModel,
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: MapMarkersViewModel = viewModel(inputModel)
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "Map Markers",
        icon = Res.drawable.window_locations,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = true,
    ) {
        MapMarkersWindowContent(
            state = state,
            onSystemTextChange = viewModel::onSystemTextChange,
            onAddMarkerClick = viewModel::onAddMarkerClick,
            onDeleteMarkerClick = viewModel::onDeleteMarkerClick,
            onCancelEditClick = viewModel::onCancelEditClick,
            onMarkerClick = viewModel::onMarkerClick,
            onShowMarkerClick = viewModel::onShowMarkerClick,
            onEditMarkerClick = viewModel::onEditMarkerClick,
            onToggleMarker = viewModel::onToggleMarker,
            onToggleMarkerPinned = viewModel::onToggleMarkerPinned,
            onGroupChange = viewModel::onGroupChange,
            onGroupClick = viewModel::onGroupClick,
            onCreateGroupClick = viewModel::onCreateGroupClick,
            onGroupRenameClick = viewModel::onGroupRenameClick,
            onGroupDeleteClick = viewModel::onGroupDeleteClick,
            onGroupToggleMarkers = viewModel::onGroupToggleMarkers,
        )

        val isCreateGroupDialogOpen = state.isCreateGroupDialogOpen
        if (isCreateGroupDialogOpen != null) {
            CreateGroupDialog(
                inputModel = isCreateGroupDialogOpen,
                parentWindowState = windowState,
                description = "Groups allow you to organize your markers.",
                onDismiss = viewModel::onCloseCreateGroup,
                onConfirmClick = viewModel::onCreateGroupConfirm,
            )
        }

        state.dialog?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MapMarkersWindowContent(
    state: UiState,
    onSystemTextChange: (String) -> Unit,
    onAddMarkerClick: (NewMarkerInput) -> Boolean,
    onDeleteMarkerClick: (UUID) -> Unit,
    onCancelEditClick: () -> Unit,
    onMarkerClick: (UUID) -> Unit,
    onShowMarkerClick: (UUID) -> Unit,
    onEditMarkerClick: (UUID) -> Unit,
    onToggleMarker: (UUID, Boolean) -> Unit,
    onToggleMarkerPinned: (UUID) -> Unit,
    onGroupChange: (UUID, String?) -> Unit,
    onGroupClick: (String?) -> Unit,
    onCreateGroupClick: () -> Unit,
    onGroupRenameClick: (String) -> Unit,
    onGroupDeleteClick: (String) -> Unit,
    onGroupToggleMarkers: (String?) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        ScrollbarLazyColumn(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, RiftTheme.colors.borderGrey),
            scrollbarModifier = Modifier.padding(vertical = Spacing.small),
        ) {
            if (state.markers.isEmpty()) {
                item {
                    Text(
                        text = "No map markers created",
                        style = RiftTheme.typography.headerPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.large)
                            .padding(horizontal = Spacing.large),
                    )
                }
            } else {
                val nonEmptyGroups = state.markers.mapNotNull { it.group }.toSet()
                val emptyGroups = state.groups - nonEmptyGroups
                state.markers
                    .groupBy { it.group }
                    .let { it + emptyGroups.associateWith { emptyList() } }
                    .entries
                    .sortedWith(compareBy({ it.key == null }, { it.key }))
                    .forEach { (group, markersInGroup) ->
                        val isExpanded = group !in state.collapsedGroups
                        stickyHeader {
                            val text = buildAnnotatedString {
                                withColor(RiftTheme.colors.textPrimary) {
                                    append(group ?: "Default")
                                }
                                val total = markersInGroup.size
                                val enabled = markersInGroup.count { it.isEnabled }
                                append(" - ")
                                append(total.toString())
                                append(" marker${total.plural}")
                                if (enabled < total) {
                                    append(" - ")
                                    append(enabled.toString())
                                    append(" enabled")
                                }
                            }
                            MarkerGroupHeader(
                                name = text,
                                isEmpty = markersInGroup.isEmpty(),
                                isExpanded = isExpanded,
                                isDefault = group == null,
                                hasEnabledMarkers = markersInGroup.any { it.isEnabled },
                                onClick = { onGroupClick(group) },
                                onGroupToggleMarkers = { onGroupToggleMarkers(group) },
                                onGroupRenameClick = { onGroupRenameClick(group!!) },
                                onGroupDeleteClick = { onGroupDeleteClick(group!!) },
                            )
                        }
                        if (isExpanded) {
                            if (group in emptyGroups) {
                                item {
                                    EmptyGroup()
                                }
                            }
                            items(markersInGroup, key = { it.id }) { marker ->
                                MarkerItem(
                                    marker = marker,
                                    isExpanded = marker.id == state.expandedMarker,
                                    groups = state.groups,
                                    onMarkerClick = { onMarkerClick(marker.id) },
                                    onShowMarkerClick = { onShowMarkerClick(marker.id) },
                                    onEditMarkerClick = { onEditMarkerClick(marker.id) },
                                    onDeleteMarkerClick = { onDeleteMarkerClick(marker.id) },
                                    onToggleMarker = { onToggleMarker(marker.id, it) },
                                    onToggleMarkerPinned = { onToggleMarkerPinned(marker.id) },
                                    onGroupChange = { onGroupChange(marker.id, it) },
                                )
                            }
                        }
                    }
            }
        }

        var markerText by remember { mutableStateOf("") }
        var markerColor: Color? by remember { mutableStateOf(null) }
        var markerIcon by remember { mutableStateOf("map_marker_place_bookmark") }

        LaunchedEffect(state.editingMarker) {
            state.editingMarker?.let {
                markerText = it.label
                markerColor = it.color
                markerIcon = it.iconName
            } ?: run {
                markerText = ""
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
            RiftAutocompleteTextField(
                text = state.systemText,
                suggestions = remember(state.systemText) { getSuggestions(solarSystemsRepository, state.systemText) },
                placeholder = "System name",
                onTextChanged = {
                    onSystemTextChange(it)
                },
                onDeleteClick = { onSystemTextChange("") },
                modifier = Modifier.weight(1f),
            )
            RiftTextField(
                text = markerText,
                placeholder = "Label",
                onTextChanged = { markerText = it.take(50) },
                onDeleteClick = { markerText = "" },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Spacer(Modifier.weight(1f))
            if (state.markers.isNotEmpty()) {
                RiftButton(
                    text = "Create group",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = onCreateGroupClick,
                )
            }
            val isEditing = state.editingMarker != null
            if (isEditing) {
                RiftButton(
                    text = "Cancel",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = onCancelEditClick,
                )
            }
            RiftButton(
                text = if (isEditing) "Update" else "Add marker",
                onClick = {
                    val added = onAddMarkerClick(NewMarkerInput(state.systemText, markerText, markerColor, markerIcon))
                    if (added) {
                        onSystemTextChange("")
                        markerText = ""
                    }
                },
            )
        }

        Text(
            text = "Marker color",
            style = RiftTheme.typography.headerPrimary,
        )
        val animatedColor by animateColorAsState(markerColor ?: Color.White)
        val colors = listOf(
            null,
            Color(0xFF2C75E1),
            Color(0xFF399AEB),
            Color(0xFF4ECEF8),
            Color(0xFF60DBA3),
            Color(0xFF71E754),
            Color(0xFFF5FF83),
            Color(0xFFDC6C06),
            Color(0xFFCE440F),
            Color(0xFFBB1116),
            Color(0xFF731F1F),
            Color(0xFF8D3163),
        )
        Row(
            modifier = Modifier
                .border(1.dp, RiftTheme.colors.borderGreyLight),
        ) {
            for (color in colors) {
                Image(
                    painter = painterResource(Res.drawable.flag_background),
                    contentDescription = null,
                    colorFilter = color?.let { ColorFilter.tint(it) },
                    modifier = Modifier
                        .hoverBackground()
                        .modifyIf(color == markerColor) {
                            background(RiftTheme.colors.backgroundSelected)
                        }
                        .onClick {
                            markerColor = color
                        }
                        .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                        .padding(6.dp)
                        .size(12.dp),
                )
            }
        }

        Text(
            text = "Marker icon",
            style = RiftTheme.typography.headerPrimary,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Res.allDrawableResources
                .filter { (name, _) -> name.startsWith("map_marker_") }
                .entries
                .groupBy { it.key.substringAfter("map_marker_").substringBefore("_") }
                .forEach { (_, markers) ->
                    Row(
                        modifier = Modifier
                            .border(1.dp, RiftTheme.colors.borderGreyLight),
                    ) {
                        for ((name, drawable) in markers) {
                            Image(
                                painter = painterResource(drawable),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(animatedColor),
                                modifier = Modifier
                                    .hoverBackground()
                                    .modifyIf(name == markerIcon) {
                                        background(RiftTheme.colors.backgroundSelected)
                                    }
                                    .onClick {
                                        markerIcon = name
                                    }
                                    .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                                    .padding(Spacing.small)
                                    .size(16.dp),
                            )
                        }
                    }
                }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.MarkerGroupHeader(
    name: AnnotatedString,
    isEmpty: Boolean,
    isExpanded: Boolean,
    isDefault: Boolean,
    hasEnabledMarkers: Boolean,
    onClick: () -> Unit,
    onGroupToggleMarkers: () -> Unit,
    onGroupRenameClick: () -> Unit,
    onGroupDeleteClick: () -> Unit,
) {
    val pointerState = remember { PointerInteractionStateHolder() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pointerInteraction(pointerState)
            .background(RiftTheme.colors.backgroundPrimary)
            .fillMaxWidth()
            .animateItem()
            .animateContentSize()
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .onClick { onClick() },
    ) {
        ExpandChevron(isExpanded = isExpanded)
        Text(
            text = name,
            style = RiftTheme.typography.headerSecondary,
            modifier = Modifier.padding(vertical = Spacing.small),
        )
        Spacer(Modifier.weight(1f))

        val buttonsAlpha by animateFloatAsState(if (pointerState.isHovered) 1f else 0f)
        if (!isEmpty) {
            RiftTooltipArea(
                text = if (hasEnabledMarkers) "Disable all markers" else "Enable all markers",
            ) {
                RiftImageButton(
                    resource = if (hasEnabledMarkers) Res.drawable.toggle_on_18 else Res.drawable.toggle_off_18,
                    size = 18.dp,
                    onClick = onGroupToggleMarkers,
                    modifier = Modifier.alpha(buttonsAlpha),
                )
            }
        }
        if (!isDefault) {
            RiftTooltipArea(
                text = "Rename group",
            ) {
                RiftImageButton(
                    resource = Res.drawable.editplanicon,
                    size = 20.dp,
                    onClick = onGroupRenameClick,
                    modifier = Modifier.alpha(buttonsAlpha),
                )
            }
            RiftTooltipArea(
                text = if (isEmpty) "Delete group" else "Delete group and move markers to default",
            ) {
                RiftImageButton(
                    resource = Res.drawable.delete,
                    size = 20.dp,
                    onClick = onGroupDeleteClick,
                    modifier = Modifier.alpha(buttonsAlpha),
                )
            }
        }
        Spacer(Modifier.width(Spacing.small))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.EmptyGroup() {
    Row(
        modifier = Modifier
            .padding(vertical = Spacing.medium, horizontal = Spacing.medium)
            .fillMaxWidth()
            .animateItem()
            .animateContentSize(),
    ) {
        Text(
            text = "No map markers in this group",
            style = RiftTheme.typography.bodySecondary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.MarkerItem(
    marker: MapMarkerItem,
    isExpanded: Boolean,
    groups: Set<String>,
    onMarkerClick: () -> Unit,
    onShowMarkerClick: () -> Unit,
    onEditMarkerClick: () -> Unit,
    onDeleteMarkerClick: () -> Unit,
    onToggleMarker: (Boolean) -> Unit,
    onToggleMarkerPinned: () -> Unit,
    onGroupChange: (String?) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier
            .hoverBackground()
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .padding(vertical = Spacing.medium)
            .fillMaxWidth()
            .animateItem()
            .animateContentSize()
            .onClick { onMarkerClick() },
    ) {
        val alpha = if (marker.isEnabled) 1f else 0.5f
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.small),
        ) {
            RiftCheckbox(
                isChecked = marker.isEnabled,
                onCheckedChange = onToggleMarker,
            )
            Image(
                painter = painterResource(marker.icon),
                contentDescription = null,
                colorFilter = marker.color?.let { ColorFilter.tint(it) },
                modifier = Modifier
                    .alpha(alpha)
                    .size(16.dp),
            )
            RiftTooltipArea(
                tooltip = @Composable {
                    Column(
                        modifier = Modifier.padding(Spacing.large),
                    ) {
                        Text(
                            text = if (marker.isPinned) "Unpin marker" else "Pin marker",
                            style = RiftTheme.typography.bodyPrimary,
                        )
                        Text(
                            text = "Pinned systems are visible on all zoom levels",
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                },
            ) {
                RiftImageButton(
                    resource = Res.drawable.menu_pinned,
                    size = 16.dp,
                    onClick = onToggleMarkerPinned,
                    isFullAlpha = marker.isPinned,
                    modifier = Modifier.alpha(if (marker.isPinned) alpha else alpha * 0.35f),
                )
            }
            Text(
                text = "${marker.systemName} (${marker.regionName})",
                style = RiftTheme.typography.bodyHighlighted,
                maxLines = 1,
                modifier = Modifier.alpha(alpha),
            )
            Text(
                text = marker.label,
                style = RiftTheme.typography.bodyPrimary,
                modifier = Modifier
                    .weight(1f)
                    .alpha(alpha),
            )
        }
        if (isExpanded) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .padding(horizontal = Spacing.medium)
                    .fillMaxWidth(),
            ) {
                RiftDropdownWithLabel(
                    label = "Group:",
                    items = (groups.sorted() + listOf(null)).toList(),
                    selectedItem = marker.group,
                    onItemSelected = onGroupChange,
                    getItemName = { it ?: "Default" },
                    maxItems = 3,
                    modifier = Modifier
                        .widthIn(max = 170.dp)
                        .padding(end = Spacing.medium),
                )
                RiftButton(
                    text = "Show",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = onShowMarkerClick,
                    modifier = Modifier.padding(end = Spacing.medium),
                )
                RiftButton(
                    text = "Edit",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = onEditMarkerClick,
                    modifier = Modifier.padding(end = Spacing.medium),
                )
                RiftButton(
                    text = "Delete",
                    type = ButtonType.Negative,
                    onClick = onDeleteMarkerClick,
                )
            }
        }
    }
}

private fun getSuggestions(solarSystemsRepository: SolarSystemsRepository, text: String): List<String> {
    solarSystemsRepository.getSystem(text)?.let { return emptyList() }
    return solarSystemsRepository.getSystems()
        .asSequence()
        .map { it.name }
        .filter { it.lowercase().startsWith(text.lowercase()) }
        .take(5)
        .toList()
}
