package dev.nohus.rift.assets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.assets.AssetsViewModel.Asset
import dev.nohus.rift.assets.AssetsViewModel.AssetLocation
import dev.nohus.rift.assets.AssetsViewModel.FitAction
import dev.nohus.rift.assets.AssetsViewModel.SortType
import dev.nohus.rift.assets.AssetsViewModel.UiState
import dev.nohus.rift.assets.FittingController.Fitting
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.ExpandChevron
import dev.nohus.rift.compose.GetSystemContextMenuItems
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftContextMenuArea
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftSearchField
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.menu_eye
import dev.nohus.rift.generated.resources.menu_favorite
import dev.nohus.rift.generated.resources.menu_uneye
import dev.nohus.rift.generated.resources.menu_unfavorite
import dev.nohus.rift.generated.resources.window_assets
import dev.nohus.rift.map.SecurityColors
import dev.nohus.rift.settings.persistence.LocationPinStatus
import dev.nohus.rift.utils.formatIsk
import dev.nohus.rift.utils.formatNumberCompact
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.roundSecurity
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import java.text.NumberFormat

@Composable
fun AssetsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: AssetsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Assets",
        icon = Res.drawable.window_assets,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        AssetsWindowContent(
            state = state,
            onCharacterSelected = viewModel::onCharacterSelected,
            onSortSelected = viewModel::onSortSelected,
            onSearchChange = viewModel::onSearchChange,
            onFitAction = viewModel::onFitAction,
            onPinChange = viewModel::onPinChange,
            onReloadClick = viewModel::onReloadClick,
        )
    }
}

@Composable
private fun AssetsWindowContent(
    state: UiState,
    onCharacterSelected: (LocalCharacter?) -> Unit,
    onSortSelected: (SortType) -> Unit,
    onSearchChange: (String) -> Unit,
    onFitAction: (Fitting, FitAction) -> Unit,
    onPinChange: (Long, LocationPinStatus) -> Unit,
    onReloadClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.padding(bottom = Spacing.medium),
        ) {
            RiftDropdown(
                items = listOf(null) + state.characters,
                selectedItem = state.filterCharacter,
                onItemSelected = onCharacterSelected,
                getItemName = {
                    if (it != null) {
                        it.info.success?.name ?: "${it.characterId}"
                    } else {
                        "All characters"
                    }
                },
                modifier = Modifier.widthIn(max = 150.dp),
            )
            Spacer(Modifier.width(Spacing.medium))
            RiftDropdownWithLabel(
                label = "Sort By",
                items = SortType.entries,
                selectedItem = state.sort,
                onItemSelected = onSortSelected,
                getItemName = {
                    when (it) {
                        SortType.Distance -> "Distance"
                        SortType.Name -> "Name"
                        SortType.Count -> "Asset count"
                        SortType.Price -> "Total price"
                    }
                },
            )
            Spacer(Modifier.weight(1f))
            RiftSearchField(
                search = state.search,
                isCompact = false,
                onSearchChange = onSearchChange,
            )
        }
        var expandedLocations by remember { mutableStateOf<Set<AssetLocation>>(emptySet()) }
        var expandedItems by remember { mutableStateOf<Set<Long>>(emptySet()) }
        ScrollbarLazyColumn {
            state.assetTotals?.let { totals ->
                item(key = "totals") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RiftTheme.colors.windowBackgroundSecondary)
                            .padding(start = 24.dp)
                            .padding(vertical = Spacing.small),
                    ) {
                        val text = buildAnnotatedString {
                            append("Total: ")
                            append("${totals.locations} Location${totals.locations.plural}")
                            append(" - ")
                            append("${totals.items} Item${totals.items.plural}")
                            append(" - ")
                            append(formatIsk(totals.price))
                            append(" - ")
                            append(formatNumberCompact(totals.volume) + " m3")
                        }
                        Text(
                            text = text,
                            style = RiftTheme.typography.bodySecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            softWrap = false,
                            modifier = Modifier.clipToBounds(),
                        )
                    }
                }
            }
            val characterNames: Map<Int, String>? = if (state.filterCharacter == null) {
                state.characters.mapNotNull { it.characterId to (it.info.success?.name ?: return@mapNotNull null) }.toMap()
            } else {
                null
            }
            var previousPinStatus: LocationPinStatus? = null
            state.assets.forEach { (location, assets) ->
                val pinStatus = state.pins[location.locationId] ?: LocationPinStatus.None
                if (previousPinStatus != pinStatus) {
                    item(key = "pin-divider-$previousPinStatus") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp)
                                .padding(vertical = Spacing.small),
                        ) {
                        }
                    }
                }
                previousPinStatus = pinStatus

                val isLocationExpanded = location in expandedLocations
                item(key = location.locationId) {
                    LocationHeader(
                        location = location,
                        assets = assets,
                        isExpanded = isLocationExpanded,
                        expandedItems = expandedItems,
                        characterNames = characterNames,
                        pinStatus = pinStatus,
                        onClick = {
                            if (isLocationExpanded) expandedLocations -= location else expandedLocations += location
                        },
                        onItemClick = { itemId ->
                            if (itemId in expandedItems) expandedItems -= itemId else expandedItems += itemId
                        },
                        onFitAction = onFitAction,
                        onPinChange = { onPinChange(location.locationId, it) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            item(key = { "footer" }) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                    modifier = Modifier.animateItem(),
                ) {
                    if (state.assets.isEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.medium),
                        ) {
                            if (state.search.isNotBlank()) {
                                Text("No matching assets found")
                            } else if (state.filterCharacter != null) {
                                Text("No assets loaded for this character")
                            } else {
                                Text("No assets loaded")
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.medium),
                    ) {
                        if (state.assets.isNotEmpty()) {
                            Text("Delayed up to 1 hour")
                        }
                        AnimatedContent(
                            state.isLoading,
                            modifier = Modifier
                                .height(36.dp)
                                .padding(start = Spacing.medium),
                        ) {
                            if (it) {
                                LoadingSpinner(
                                    modifier = Modifier.size(36.dp),
                                )
                            } else {
                                RiftButton(
                                    text = "Reload",
                                    type = ButtonType.Secondary,
                                    onClick = onReloadClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocationHeader(
    location: AssetLocation,
    assets: List<Asset>,
    isExpanded: Boolean,
    expandedItems: Set<Long>,
    characterNames: Map<Int, String>?,
    pinStatus: LocationPinStatus,
    onClick: () -> Unit,
    onItemClick: (itemId: Long) -> Unit,
    onFitAction: (Fitting, FitAction) -> Unit,
    onPinChange: (LocationPinStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        val contextMenuItems = buildList {
            add(ContextMenuItem.DividerItem)
            if (pinStatus == LocationPinStatus.Pinned) {
                add(
                    ContextMenuItem.TextItem(
                        text = "Unpin in Assets",
                        iconResource = Res.drawable.menu_unfavorite,
                        onClick = { onPinChange(LocationPinStatus.None) },
                    ),
                )
            } else {
                add(
                    ContextMenuItem.TextItem(
                        text = "Pin in Assets",
                        iconResource = Res.drawable.menu_favorite,
                        onClick = { onPinChange(LocationPinStatus.Pinned) },
                    ),
                )
            }
            if (pinStatus == LocationPinStatus.Hidden) {
                add(
                    ContextMenuItem.TextItem(
                        text = "Unhide in Assets",
                        iconResource = Res.drawable.menu_uneye,
                        onClick = { onPinChange(LocationPinStatus.None) },
                    ),
                )
            } else {
                add(
                    ContextMenuItem.TextItem(
                        text = "Hide in Assets",
                        iconResource = Res.drawable.menu_eye,
                        onClick = { onPinChange(LocationPinStatus.Hidden) },
                    ),
                )
            }
        }
        RiftContextMenuArea(
            items = GetSystemContextMenuItems(
                systemId = location.systemId,
                locationId = location.locationId,
            ) + contextMenuItems,
            modifier = Modifier.pointerHoverIcon(PointerIcon(Cursors.pointerInteractive)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RiftTheme.colors.windowBackgroundSecondary)
                    .hoverBackground()
                    .padding(vertical = Spacing.small)
                    .onClick { onClick() },
            ) {
                ExpandChevron(isExpanded = isExpanded)
                val text = buildAnnotatedString {
                    location.security?.let {
                        withStyle(style = SpanStyle(color = SecurityColors[it], fontWeight = FontWeight.Bold)) {
                            append(it.roundSecurity().toString())
                        }
                        append(" ")
                    }
                    append(location.name)
                    append(" - ")
                    append("${assets.size} Item${if (assets.size != 1) "s" else ""}")
                    append(" - ")
                    val totalPrice = assets.sumOf { it.getTotalPrice() }
                    append(formatIsk(totalPrice))
                    append(" - ")
                    val totalVolume = assets.sumOf { it.getTotalVolume() }
                    append(formatNumberCompact(totalVolume) + " m3")
                    location.distance?.let {
                        append(" - ")
                        append("Route: $it Jump${if (it != 1) "s" else ""}")
                    }
                }
                Text(
                    text = text,
                    style = RiftTheme.typography.bodyPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                    modifier = Modifier.clipToBounds(),
                )
            }
        }
        AnimatedVisibility(isExpanded) {
            Column {
                assets.forEach { asset ->
                    key(asset.asset.itemId) {
                        AssetRow(
                            asset = asset,
                            expandedItems = expandedItems,
                            characterNames = characterNames,
                            onClick = onItemClick,
                            onFitAction = onFitAction,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssetRow(
    asset: Asset,
    expandedItems: Set<Long>,
    depth: Int = 1,
    characterNames: Map<Int, String>?,
    onClick: (Long) -> Unit,
    onFitAction: (Fitting, FitAction) -> Unit,
) {
    Column {
        val isExpanded = asset.asset.itemId in expandedItems
        val depthOffset = 24.dp * if (asset.children.isNotEmpty()) (depth - 1) else depth
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .hoverBackground()
                .padding(vertical = Spacing.small)
                .padding(start = depthOffset)
                .onClick { onClick(asset.asset.itemId) },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (asset.children.isNotEmpty()) {
                    ExpandChevron(isExpanded = isExpanded)
                }
                AsyncTypeIcon(
                    type = asset.type,
                    modifier = Modifier.size(32.dp),
                )
                Column(
                    modifier = Modifier.padding(start = Spacing.medium),
                ) {
                    val text = buildAnnotatedString {
                        asset.name?.takeIf { it.isNotBlank() }?.let {
                            append("$it - ")
                        }
                        append(asset.typeName.trim())
                        asset.type?.volume?.let { volume ->
                            val formatted = NumberFormat.getNumberInstance().format(asset.asset.quantity * volume)
                            withStyle(style = SpanStyle(color = RiftTheme.colors.textSecondary)) {
                                append(" - $formatted m3")
                            }
                        }
                        if (asset.price != null) {
                            withStyle(style = SpanStyle(color = RiftTheme.colors.textSecondary)) {
                                append(" - ${formatIsk(asset.price * asset.asset.quantity)}")
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (depth == 1 && characterNames != null) {
                            RiftTooltipArea(
                                text = "${characterNames[asset.characterId] ?: asset.characterId}",
                                modifier = Modifier.padding(end = Spacing.small),
                            ) {
                                AsyncPlayerPortrait(
                                    characterId = asset.characterId,
                                    size = 32,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, RiftTheme.colors.borderGrey, CircleShape),
                                )
                            }
                        }
                        Text(
                            text = text,
                            style = RiftTheme.typography.bodyPrimary,
                        )
                    }
                    val secondaryText = buildList {
                        val flag = LocationFlags.getName(asset.asset.locationFlag)
                        if (flag != null) {
                            add(flag)
                        }
                        if (asset.asset.quantity > 1) {
                            val formatted = NumberFormat.getIntegerInstance().format(asset.asset.quantity)
                            add("$formatted units")
                        }
                        if (asset.children.isNotEmpty()) {
                            add("${asset.children.size} item${if (asset.children.size != 1) "s" else ""}")
                            val volume = asset.children.map { it.asset.quantity * (it.type?.volume ?: 0f) }.sum()
                            val formatted = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 1 }.format(volume)
                            add("$formatted m3")

                            val totalPrice = asset.children.sumOf { it.getTotalPrice() }
                            add(formatIsk(totalPrice))
                        }
                    }.joinToString(" - ")
                    if (secondaryText.isNotEmpty()) {
                        Text(
                            text = secondaryText,
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
            }
            AnimatedVisibility(isExpanded && asset.fitting != null) {
                if (asset.fitting != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        modifier = Modifier
                            .padding(top = Spacing.small)
                            .padding(start = 24.dp),
                    ) {
                        RiftButton(
                            text = "Copy fit",
                            type = ButtonType.Secondary,
                            cornerCut = ButtonCornerCut.BottomLeft,
                            onClick = { onFitAction(asset.fitting, FitAction.Copy) },
                        )
                        RiftButton(
                            text = "Copy fit & cargo",
                            type = ButtonType.Secondary,
                            cornerCut = ButtonCornerCut.None,
                            onClick = { onFitAction(asset.fitting, FitAction.CopyWithCargo) },
                        )
                        RiftButton(
                            text = "View fit",
                            cornerCut = ButtonCornerCut.BottomRight,
                            onClick = { onFitAction(asset.fitting, FitAction.Open) },
                        )
                    }
                }
            }
        }
        AnimatedVisibility(isExpanded) {
            Column {
                asset.children.forEach { child ->
                    AssetRow(
                        asset = child,
                        expandedItems = expandedItems,
                        depth = depth + 1,
                        characterNames = characterNames,
                        onClick = onClick,
                        onFitAction = onFitAction,
                    )
                }
            }
        }
    }
}
