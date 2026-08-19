package dev.nohus.rift.structures

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.OnVisibilityChange
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_structures
import dev.nohus.rift.structures.EquinoxStructuresRepository.Skyhook
import dev.nohus.rift.structures.EquinoxStructuresRepository.SkyhookResource
import dev.nohus.rift.structures.StructuresViewModel.SkyhookResourceFilter
import dev.nohus.rift.structures.StructuresViewModel.StructuresTab
import dev.nohus.rift.structures.StructuresViewModel.UiState
import dev.nohus.rift.structures.compose.MercenaryDen
import dev.nohus.rift.structures.compose.SovereigntyHub
import dev.nohus.rift.structures.compose.StructuresLoadingProgress
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.structures.compose.Skyhook as SkyhookCard

@Composable
fun StructuresWindow(
    windowState: WindowManager.RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: StructuresViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Structures",
        icon = Res.drawable.window_structures,
        state = windowState,
        onCloseClick = onCloseRequest,
        titleBarContent = { height ->
            ToolbarRow(
                state = state,
                fixedHeight = height,
                onTabSelected = viewModel::onTabSelected,
            )
        },
        withContentPadding = false,
    ) {
        StructuresWindowContent(
            state = state,
            onSkyhookResourceFilterSelected = viewModel::onSkyhookResourceFilterSelected,
            onViewOperationClick = viewModel::onViewOperationClick,
        )
        OnVisibilityChange(viewModel::onVisibilityChange)
    }
}

@Composable
fun ToolbarRow(
    state: UiState,
    fixedHeight: Dp,
    onTabSelected: (StructuresTab) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RiftTabBar(
            tabs = listOf(
                Tab(
                    id = StructuresTab.Skyhooks.ordinal,
                    title = "Skyhooks",
                    isCloseable = false,
                ),
                Tab(
                    id = StructuresTab.SovereigntyHubs.ordinal,
                    title = "Sovereignty Hubs",
                    isCloseable = false,
                ),
                Tab(
                    id = StructuresTab.MercenaryDens.ordinal,
                    title = "Mercenary Dens",
                    isCloseable = false,
                ),
            ),
            selectedTab = state.selectedTab.ordinal,
            onTabSelected = { tab ->
                onTabSelected(StructuresTab.entries.firstOrNull { it.ordinal == tab } ?: StructuresTab.Skyhooks)
            },
            onTabClosed = {},
            withUnderline = false,
            withWideTabs = true,
            fixedHeight = fixedHeight,
        )
    }
}

@Composable
private fun StructuresWindowContent(
    state: UiState,
    onSkyhookResourceFilterSelected: (SkyhookResourceFilter) -> Unit,
    onViewOperationClick: (id: String) -> Unit,
) {
    Column {
        val offset = LocalDensity.current.run { 1.dp.toPx() }
        Box(
            modifier = Modifier
                .graphicsLayer(translationY = -offset)
                .fillMaxWidth()
                .height(1.dp)
                .background(RiftTheme.colors.borderGreyLight),
        )

        if (state.loading.stage != null && state.structures == null) {
            StructuresLoadingProgress(state.loading, state.loading.stage)
        } else {
            LoadedState(state, onSkyhookResourceFilterSelected, onViewOperationClick)
        }
    }
}

@Composable
private fun LoadedState(
    state: UiState,
    onSkyhookResourceFilterSelected: (SkyhookResourceFilter) -> Unit,
    onViewOperationClick: (id: String) -> Unit,
) {
    Box(
        modifier = Modifier.padding(Spacing.large),
    ) {
        val now = getNow()
        val structures = state.structures

        when (state.selectedTab) {
            StructuresTab.Skyhooks -> {
                if (structures?.skyhooks?.isNotEmpty() == true) {
                    val filteredSkyhooks = structures.skyhooks.filter { it.matches(state.skyhookResourceFilter) }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        SkyhookResourceFilterDropdown(
                            selectedFilter = state.skyhookResourceFilter,
                            onFilterSelected = onSkyhookResourceFilterSelected,
                        )
                        if (filteredSkyhooks.isNotEmpty()) {
                            ScrollbarLazyColumn(
                                verticalArrangement = Arrangement.spacedBy(Spacing.veryLarge),
                                modifier = Modifier.weight(1f),
                            ) {
                                items(filteredSkyhooks) { item ->
                                    SkyhookCard(item, now, Modifier.animateItem())
                                }
                            }
                        } else {
                            EmptyState("All Skyhooks filtered out", "Select a different resource type to see matching Skyhooks.")
                        }
                    }
                } else {
                    EmptyState("No Skyhooks found", "Add a character with the Station Manager role in a corporation with Skyhooks.")
                }
            }
            StructuresTab.SovereigntyHubs -> {
                if (structures?.sovHubs?.isNotEmpty() == true) {
                    ScrollbarLazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Spacing.veryLarge),
                    ) {
                        items(structures.sovHubs) { item ->
                            SovereigntyHub(item, now, Modifier.animateItem())
                        }
                    }
                } else {
                    EmptyState("No Sovereignty Hubs found", "Add a character with the Station Manager role in a corporation with Sovereignty Hubs.")
                }
            }
            StructuresTab.MercenaryDens -> {
                if (structures?.mercenaryDens?.isNotEmpty() == true) {
                    ScrollbarLazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Spacing.veryLarge),
                    ) {
                        items(structures.mercenaryDens) { item ->
                            val operations = state.operations?.operations?.filter { it.mercenaryDen == item } ?: emptyList()
                            MercenaryDen(item, operations, now, onViewOperationClick, Modifier.animateItem())
                        }
                    }
                } else {
                    EmptyState("No Mercenary Dens found", "Deploy a Mercenary Den to see it here.")
                }
            }
        }
    }
}

@Composable
private fun SkyhookResourceFilterDropdown(
    selectedFilter: SkyhookResourceFilter,
    onFilterSelected: (SkyhookResourceFilter) -> Unit,
) {
    val filters = listOf(
        SkyhookResourceFilter.All,
        SkyhookResourceFilter.Power,
        SkyhookResourceFilter.Workforce,
        SkyhookResourceFilter.ReagentGas,
        SkyhookResourceFilter.ReagentIce,
    )
    RiftDropdownWithLabel(
        label = "Resource filter",
        items = filters,
        selectedItem = selectedFilter,
        onItemSelected = onFilterSelected,
        getItemName = { it.displayName },
    )
}

private fun Skyhook.matches(filter: SkyhookResourceFilter): Boolean {
    return when (filter) {
        SkyhookResourceFilter.All -> true
        SkyhookResourceFilter.Power -> resource is SkyhookResource.Power
        SkyhookResourceFilter.Workforce -> resource is SkyhookResource.Workforce
        SkyhookResourceFilter.ReagentGas -> resource is SkyhookResource.Reagent && resource.name == "Magmatic Gas"
        SkyhookResourceFilter.ReagentIce -> resource is SkyhookResource.Reagent && resource.name == "Superionic Ice"
    }
}

private val SkyhookResourceFilter.displayName: String
    get() = when (this) {
        SkyhookResourceFilter.All -> "All"
        SkyhookResourceFilter.Power -> "Power"
        SkyhookResourceFilter.Workforce -> "Workforce"
        SkyhookResourceFilter.ReagentGas -> "Magmatic Gas"
        SkyhookResourceFilter.ReagentIce -> "Superionic Ice"
    }

@Composable
private fun EmptyState(text: String, description: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    ) {
        Text(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.medium))
        Text(
            text = description,
            style = RiftTheme.typography.bodySecondary,
            textAlign = TextAlign.Center,
        )
    }
}
