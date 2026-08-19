package dev.nohus.rift.opportunities.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.RiftSideNavigation
import dev.nohus.rift.compose.RiftSideNavigationHeader
import dev.nohus.rift.compose.RiftSideNavigationItem
import dev.nohus.rift.compose.RiftToggleButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftVerticalGlowLine
import dev.nohus.rift.compose.Side
import dev.nohus.rift.compose.ToggleButtonType
import dev.nohus.rift.compose.fadingRightEdge
import dev.nohus.rift.compose.getActiveWindowTransitionSpec
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.contact_tag
import dev.nohus.rift.generated.resources.house_16px
import dev.nohus.rift.opportunities.OpportunitiesViewModel
import dev.nohus.rift.opportunities.OpportunitiesViewModel.OpportunityLifecycleFilter
import dev.nohus.rift.opportunities.OpportunityCategoryFilter
import dev.nohus.rift.opportunities.OpportunityCategoryFilterType
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun SideNavigation(
    state: OpportunitiesViewModel.UiState,
    onLifecycleFilterChange: (OpportunityLifecycleFilter) -> Unit,
    onClick: (OpportunityCategoryFilter?) -> Unit,
) {
    RiftSideNavigation(
        footer = {
            LoadingFooter(isLoading = state.isLoading)
        }
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier.padding(end = 8.dp, top = 8.dp),
            ) {
                RiftToggleButton(
                    text = "Current",
                    isSelected = state.lifecycleFilter == OpportunityLifecycleFilter.Active,
                    type = ToggleButtonType.Left,
                    onClick = { onLifecycleFilterChange(OpportunityLifecycleFilter.Active) },
                    modifier = Modifier.weight(1f),
                )
                RiftToggleButton(
                    text = "History",
                    isSelected = state.lifecycleFilter == OpportunityLifecycleFilter.History,
                    type = ToggleButtonType.Right,
                    onClick = { onLifecycleFilterChange(OpportunityLifecycleFilter.History) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        val allFilters = OpportunityCategoryFilter::class.sealedSubclasses.map { it.objectInstance!! }
        val primaryFilter = if (state.participatingFilter) null else state.primaryFilter

        item {
            RiftSideNavigationHeader("Features")
        }
        item {
            RiftSideNavigationItem(
                text = "All",
                count = state.categoryFilters.opportunityCount[null] ?: 0,
                icon = Res.drawable.house_16px,
                isSelected = primaryFilter == null,
                onClick = { onClick(null) },
            )
        }
        val features = allFilters.filter { it.type == OpportunityCategoryFilterType.Feature }
        items(features, key = { it }) {
            Item(state.categoryFilters, primaryFilter, it, onClick)
        }

        item {
            RiftSideNavigationHeader("Career Paths")
        }
        val careerPaths = allFilters.filter { it.type == OpportunityCategoryFilterType.CareerPath }
        items(careerPaths, key = { it }) {
            Item(state.categoryFilters, primaryFilter, it, onClick)
        }

        item {
            RiftSideNavigationHeader("Other Tags")
        }
        val activities = allFilters
            .filter { it.type == OpportunityCategoryFilterType.Activity }
            .filter { (state.categoryFilters.opportunityCount[it] ?: 0) > 0 || it == state.primaryFilter }
        items(activities, key = { it }) {
            Item(state.categoryFilters, primaryFilter, it, onClick)
        }
    }
}

@Composable
private fun LazyItemScope.Item(
    categoryFilters: OpportunitiesViewModel.CategoryFilters,
    primaryFilter: OpportunityCategoryFilter?,
    filter: OpportunityCategoryFilter,
    onClick: (OpportunityCategoryFilter) -> Unit,
) {
    val showIcon = filter.type in listOf(OpportunityCategoryFilterType.Feature, OpportunityCategoryFilterType.CareerPath)
    val isSelected = primaryFilter == filter
    RiftSideNavigationItem(
        text = filter.name,
        count = categoryFilters.opportunityCount[filter] ?: 0,
        icon = filter.icon.takeIf { showIcon } ?: Res.drawable.contact_tag.takeIf { isSelected },
        isSelected = isSelected,
        onClick = { onClick(filter) },
    )
}

@Composable
private fun LoadingFooter(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.medium),
    ) {
        AnimatedVisibility(isLoading) {
            RiftTooltipArea("Loading opportunities…") {
                LoadingSpinner(
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}
