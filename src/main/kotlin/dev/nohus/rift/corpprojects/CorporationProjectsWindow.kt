package dev.nohus.rift.corpprojects

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ClickableCorporation
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.LoadingSpinnerAmbient
import dev.nohus.rift.compose.OnVisibilityChange
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftContextMenuPopup
import dev.nohus.rift.compose.RiftOpportunityCard
import dev.nohus.rift.compose.RiftOpportunityCardBottomContent
import dev.nohus.rift.compose.RiftOpportunityCardButton
import dev.nohus.rift.compose.RiftOpportunityCardTopRight.RiftOpportunityCardProgressGauge
import dev.nohus.rift.compose.RiftSearchField
import dev.nohus.rift.compose.RiftToggleButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftVerticalGlowLine
import dev.nohus.rift.compose.RiftWarningBanner
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyVerticalGrid
import dev.nohus.rift.compose.SharedTransitionAnimatedContent
import dev.nohus.rift.compose.Side
import dev.nohus.rift.compose.ToggleButtonType
import dev.nohus.rift.compose.getActiveWindowTransitionSpec
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.sharedTransitionElement
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.corpprojects.CorporationProjectsUtils.getProjectCategory
import dev.nohus.rift.corpprojects.CorporationProjectsUtils.getProjectType
import dev.nohus.rift.corpprojects.CorporationProjectsViewModel.ProjectLifecycleFilter
import dev.nohus.rift.corpprojects.CorporationProjectsViewModel.ProjectSorting
import dev.nohus.rift.corpprojects.CorporationProjectsViewModel.ProjectsStats
import dev.nohus.rift.corpprojects.CorporationProjectsViewModel.UiState
import dev.nohus.rift.corpprojects.CorporationProjectsViewModel.View
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.bars_sort_ascending_16px
import dev.nohus.rift.generated.resources.corporation_project_state_checkmark_16px
import dev.nohus.rift.generated.resources.corporation_project_state_close_16px
import dev.nohus.rift.generated.resources.corporation_project_state_time_16px
import dev.nohus.rift.generated.resources.expand_less_16px
import dev.nohus.rift.generated.resources.expand_more_16px
import dev.nohus.rift.generated.resources.isk
import dev.nohus.rift.generated.resources.open_window_16px
import dev.nohus.rift.generated.resources.window_corporation
import dev.nohus.rift.network.esi.models.CorporationProjectState
import dev.nohus.rift.utils.formatDateTime
import dev.nohus.rift.utils.formatDuration
import dev.nohus.rift.utils.formatIsk
import dev.nohus.rift.utils.formatIskCompact
import dev.nohus.rift.utils.formatIskReadable
import dev.nohus.rift.utils.formatNumberCompact
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.DrawableResource
import java.time.Duration
import java.time.Instant

@Composable
fun CorporationProjectsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: CorporationProjectsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Corporation Projects",
        icon = Res.drawable.window_corporation,
        state = windowState,
        onCloseClick = onCloseRequest,
        withContentPadding = false,
    ) {
        CorporationProjectsWindowContent(
            state = state,
            onLifecycleFilterChange = viewModel::onLifecycleFilterChange,
            onCategoryFilterChange = viewModel::onCategoryFilterChange,
            onSortingChange = viewModel::onSortingChange,
            onSearchChange = viewModel::onSearchChange,
            onCorporationSelect = viewModel::onCorporationSelect,
            onReloadClick = viewModel::onReloadClick,
            onProjectClick = viewModel::onProjectClick,
            onViewInGameClick = viewModel::onViewInGameClick,
            onBackClick = viewModel::onBackClick,
        )
        OnVisibilityChange(viewModel::onVisibilityChange)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CorporationProjectsWindowContent(
    state: UiState,
    onLifecycleFilterChange: (ProjectLifecycleFilter) -> Unit = {},
    onCategoryFilterChange: (ProjectCategoryFilter) -> Unit,
    onSortingChange: (sorting: ProjectSorting) -> Unit,
    onSearchChange: (String) -> Unit,
    onCorporationSelect: (Corporation) -> Unit = {},
    onReloadClick: () -> Unit = {},
    onProjectClick: (Project) -> Unit,
    onViewInGameClick: (Project) -> Unit,
    onBackClick: () -> Unit,
) {
    if (state.loading.isLoading && state.corporations.isEmpty()) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().padding(Spacing.large),
        ) {
            LoadingSpinnerAmbient()
            Spacer(Modifier.height(Spacing.medium))
            if (state.loading.corporations.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier.animateContentSize(),
                ) {
                    state.loading.corporations.forEach { corporation ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        ) {
                            AsyncCorporationLogo(
                                corporationId = corporation.corporation.id,
                                size = 64,
                                modifier = Modifier.size(48.dp),
                            )
                            Column {
                                Text(
                                    text = corporation.corporation.name,
                                    style = RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold),
                                )

                                if (corporation.projectsCount != null) {
                                    Text(
                                        text = "Loading project details… ${corporation.loadedProjectsCount} / ${corporation.projectsCount}",
                                        style = RiftTheme.typography.headerPrimary,
                                    )
                                } else {
                                    Text(
                                        text = "Loading projects…",
                                        style = RiftTheme.typography.headerPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Loading your corporations...",
                    style = RiftTheme.typography.headerPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    } else {
        val gridState: LazyGridState = rememberLazyGridState()
        SharedTransitionAnimatedContent(
            targetState = state.view,
            contentKey = { it::class },
        ) { view ->
            when (view) {
                View.ProjectsView -> ProjectsView(
                    state = state,
                    gridState = gridState,
                    onLifecycleFilterChange = onLifecycleFilterChange,
                    onCategoryFilterChange = onCategoryFilterChange,
                    onSortingChange = onSortingChange,
                    onSearchChange = onSearchChange,
                    onCorporationSelect = onCorporationSelect,
                    onProjectClick = onProjectClick,
                    onViewInGameClick = onViewInGameClick,
                    onReloadClick = onReloadClick,
                )
                is View.DetailsView -> DetailsView(
                    project = view.project,
                    onBackClick = onBackClick,
                    onCategoryFilterClick = onCategoryFilterChange,
                    onViewInGameClick = { onViewInGameClick(view.project) },
                )
            }
        }
    }
}

@Composable
private fun ProjectsView(
    state: UiState,
    gridState: LazyGridState,
    onLifecycleFilterChange: (ProjectLifecycleFilter) -> Unit,
    onCategoryFilterChange: (ProjectCategoryFilter) -> Unit,
    onSortingChange: (ProjectSorting) -> Unit,
    onSearchChange: (String) -> Unit,
    onCorporationSelect: (Corporation) -> Unit,
    onProjectClick: (Project) -> Unit,
    onViewInGameClick: (Project) -> Unit,
    onReloadClick: () -> Unit,
) {
    Column {
        if (state.projectsStats != null) {
            StatsRow(state.projectsStats)
        }
        FiltersRow(state, onLifecycleFilterChange, onCategoryFilterChange, onSortingChange, onSearchChange, onCorporationSelect)
        val cardOffset = 7.dp
        ScrollbarLazyVerticalGrid(
            gridState = gridState,
            columns = GridCells.Adaptive(minSize = 300.dp),
            contentPadding = PaddingValues(start = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.large - cardOffset),
            scrollbarModifier = Modifier.padding(horizontal = Spacing.small),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = Spacing.large, top = Spacing.medium),
        ) {
            if (state.corporations.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState("No corporations")
                }
            }

            if (state.projects != null) {
                if (state.projects.failureCause != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.padding(start = cardOffset)) {
                            RiftWarningBanner("Failed to load projects for corporation ${state.projects.corporation.name}.\nReason: ${state.projects.failureCause.message}")
                        }
                    }
                }
                val failedProjectsCount = state.projects.failedProjects.size
                if (failedProjectsCount > 0) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.padding(start = cardOffset)) {
                            RiftWarningBanner("Failed to load $failedProjectsCount project${failedProjectsCount.plural}.\nReason: ${state.projects.failedProjects.firstOrNull()?.message}")
                        }
                    }
                }
                if (state.projects.projects.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState("No Projects found")
                    }
                }
                items(state.projects.projects, key = { it.id }) {
                    ProjectCard(
                        project = it,
                        onProjectClick = { onProjectClick(it) },
                        onViewInGameClick = { onViewInGameClick(it) },
                        modifier = Modifier
                            .sharedTransitionElement("card-${it.id}")
                            .animateItem(),
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = { "reload-footer" }) {
                Box(Modifier.padding(start = cardOffset)) {}
                ReloadFooter(
                    corporations = state.corporations,
                    isLoading = state.loading.isLoading,
                    onReloadClick = onReloadClick,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun ReloadFooter(
    corporations: List<Corporation>,
    isLoading: Boolean,
    onReloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.medium),
    ) {
        if (corporations.isNotEmpty()) {
            Text("Delayed up to 10 minutes")
        }
        AnimatedContent(
            isLoading,
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

@Composable
private fun StatsRow(
    stats: ProjectsStats,
) {
    Row(
        modifier = Modifier
            .wrapContentWidth(align = Alignment.Start, unbounded = true)
            .padding(start = Spacing.large - 7.dp, end = Spacing.large, top = Spacing.medium, bottom = Spacing.medium),
    ) {
        StatsRowItem(
            value = formatIskCompact(stats.availableToYou),
            text = "Available to you",
            color = if (stats.availableToYou > 0) EveColors.successGreen else RiftTheme.colors.textPrimary,
            tooltip = buildAnnotatedString {
                withStyle(RiftTheme.typography.headlinePrimary.toSpanStyle()) {
                    appendLine(formatIsk(stats.availableToYou))
                }
                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    append(formatIskReadable(stats.availableToYou))
                }
            },
        )
        StatsRowItem(
            value = formatIskCompact(stats.availableTotal),
            text = "Available to all members",
            color = if (stats.availableTotal > 0) EveColors.successGreen else RiftTheme.colors.textPrimary,
            tooltip = buildAnnotatedString {
                withStyle(RiftTheme.typography.headlinePrimary.toSpanStyle()) {
                    appendLine(formatIsk(stats.availableTotal))
                }
                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    append(formatIskReadable(stats.availableTotal))
                }
            },
        )
        StatsRowItem(
            value = stats.active.toString(),
            text = "Active Projects",
            suffix = "/100",
            color = EveColors.airTurquoise,
            tooltip = buildAnnotatedString {
                append("A maximum of 100 Projects\ncan be active at once")
            },
        )
        StatsRowItem(
            value = stats.completedToday.toString(),
            text = "Completed Today",
            color = if (stats.completedToday > 0) EveColors.successGreen else RiftTheme.colors.textPrimary,
            tooltip = buildAnnotatedString {
                append("Number of projects completed\nin the last 24 hours")
            },
        )
        StatsRowItem(
            value = stats.completedThisWeek.toString(),
            text = "Completed This Week",
            color = if (stats.completedThisWeek > 0) EveColors.successGreen else RiftTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun StatsRowItem(
    value: String,
    text: String,
    suffix: String? = null,
    color: Color,
    tooltip: AnnotatedString? = null,
) {
    val activeWindowTransition = updateTransition(LocalWindowInfo.current.isWindowFocused)
    val colorWindowTransitionSpec = getActiveWindowTransitionSpec<Color>()
    val color by activeWindowTransition.animateColor(colorWindowTransitionSpec) {
        if (it) color else RiftTheme.colors.textPrimary
    }
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    RiftTooltipArea(
        text = tooltip,
    ) {
        Row(
            modifier = Modifier
                .pointerInteraction(pointerInteractionStateHolder)
                .padding(end = Spacing.large)
                .height(IntrinsicSize.Max),
        ) {
            RiftVerticalGlowLine(pointerInteractionStateHolder, color, Side.Right)
            Spacer(Modifier.width(Spacing.large))
            Column {
                AnimatedContent(value) { value ->
                    Text(
                        text = buildAnnotatedString {
                            append(value)
                            if (suffix != null) {
                                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                                    append(suffix)
                                }
                            }
                        },
                        style = RiftTheme.typography.displayHighlighted,
                    )
                }
                Text(
                    text = text,
                    style = RiftTheme.typography.bodySecondary,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FiltersRow(
    state: UiState,
    onLifecycleFilterChange: (ProjectLifecycleFilter) -> Unit,
    onCategoryFilterChange: (ProjectCategoryFilter) -> Unit,
    onSortingChange: (sorting: ProjectSorting) -> Unit,
    onSearchChange: (String) -> Unit,
    onCorporationSelect: (Corporation) -> Unit,
) {
    var isFiltersShown by remember { mutableStateOf(state.enabledCategoryFilters.isNotEmpty()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.large, end = Spacing.large, bottom = Spacing.medium),
    ) {
        val stateToggles = @Composable {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                RiftToggleButton(
                    text = "Active",
                    isSelected = state.lifecycleFilter == ProjectLifecycleFilter.Active,
                    type = ToggleButtonType.Left,
                    onClick = { onLifecycleFilterChange(ProjectLifecycleFilter.Active) },
                    modifier = Modifier.width(150.dp),
                )
                RiftToggleButton(
                    text = "History",
                    isSelected = state.lifecycleFilter == ProjectLifecycleFilter.History,
                    type = ToggleButtonType.Right,
                    onClick = { onLifecycleFilterChange(ProjectLifecycleFilter.History) },
                    modifier = Modifier.width(150.dp),
                )
            }
        }
        val filters = @Composable {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier,
            ) {
                RiftSearchField(
                    search = state.search,
                    isCompact = false,
                    onSearchChange = onSearchChange,
                )

                RiftButton(
                    text = "Filters",
                    icon = if (isFiltersShown) Res.drawable.expand_less_16px else Res.drawable.expand_more_16px,
                    type = ButtonType.Secondary,
                    onClick = { isFiltersShown = !isFiltersShown },
                )

                fun ProjectSorting.getName() = when (this) {
                    ProjectSorting.Name -> "Name"
                    ProjectSorting.NameReversed -> "Name (Reversed)"
                    ProjectSorting.DateCreated -> "Date Created"
                    ProjectSorting.DateCreatedReversed -> "Date Created (Newest first)"
                    ProjectSorting.TimeRemaining -> "Time Remaining"
                    ProjectSorting.TimeRemainingReversed -> "Time Remaining (Longest first)"
                    ProjectSorting.Progress -> "Progress"
                    ProjectSorting.ProgressReversed -> "Progress (Highest first)"
                    ProjectSorting.NumberOfJumps -> "Number of Jumps"
                    ProjectSorting.NumberOfJumpsReversed -> "Number of Jumps (Farthest first)"
                }

                val sortingFilterItems: List<ContextMenuItem> = ProjectSorting.entries.map { sorting ->
                    ContextMenuItem.RadioItem(
                        text = sorting.getName(),
                        onClick = { onSortingChange(sorting) },
                        isSelected = sorting == state.sorting,
                    )
                }.chunked(2).flatMap { (a, b) ->
                    listOf(a, b, ContextMenuItem.DividerItem)
                }.dropLast(1).let {
                    listOf(ContextMenuItem.HeaderItem("Sort By")) + it
                }
                Box(contentAlignment = Alignment.BottomStart) {
                    var isShown by remember { mutableStateOf(false) }
                    RiftButton(
                        text = "Sort By",
                        icon = Res.drawable.bars_sort_ascending_16px,
                        type = ButtonType.Secondary,
                        onClick = { isShown = true },
                    )
                    if (isShown) {
                        val offset = with(LocalDensity.current) {
                            32.dp.toPx().toInt()
                        }
                        RiftContextMenuPopup(
                            items = sortingFilterItems,
                            offset = IntOffset(0, offset),
                            onDismissRequest = { isShown = false },
                        )
                    }
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier.animateContentSize(),
        ) {
            if (maxWidth > 700.dp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    stateToggles()
                    Spacer(Modifier.weight(1f))
                    filters()
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.mediumLarge),
                ) {
                    stateToggles()
                    filters()
                }
            }
        }

        AnimatedVisibility(isFiltersShown) {
            if (state.applicableCategoryFilters.isNotEmpty()) {
                val (enabled, disabled) = state.applicableCategoryFilters.partition { it in state.enabledCategoryFilters }
                val filters = enabled + disabled
                ProjectCategoryFilterChips(
                    filters = filters,
                    enabledFilters = state.enabledCategoryFilters,
                    onCategoryFilterChange = onCategoryFilterChange,
                    modifier = Modifier.padding(top = Spacing.mediumLarge),
                )
            }
        }

        val corporations = state.corporations
        val selectedCorporation = state.projects?.corporation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = Spacing.mediumLarge),
        ) {
            LazyRow {
                items(corporations.sortedBy { it == selectedCorporation }, key = { it.id }) { corporation ->
                    RiftTooltipArea(corporation.name, modifier = Modifier.animateItem()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.mediumLarge),
                            modifier = Modifier
                                .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                                .onClick { onCorporationSelect(corporation) },
                        ) {
                            AsyncCorporationLogo(
                                corporationId = corporation.id,
                                size = 64,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(Spacing.mediumLarge))
            AnimatedContent(selectedCorporation) { corporation ->
                if (corporation != null) {
                    ClickableCorporation(corporation.id) {
                        LinkText(
                            text = corporation.name,
                            normalStyle = RiftTheme.typography.headlinePrimary.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            hoveredStyle = RiftTheme.typography.headlinePrimary.copy(
                                color = RiftTheme.colors.textLink,
                                fontWeight = FontWeight.Bold,
                            ),
                            hasHoverCursor = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onProjectClick: () -> Unit,
    onViewInGameClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = getProjectCategory(project)
    val type = getProjectType(project)

    val bottomContent = getProjectBottomContent(project)

    val progressGauge = RiftOpportunityCardProgressGauge(
        currentProgress = project.currentProgress,
        desiredProgress = project.desiredProgress,
        ownProgress = project.contributions.sumOf { it.contribution.success ?: 0 },
        participationLimit = project.details.participationLimit,
        state = project.state,
    )

    val buttons = buildList {
        add(
            RiftOpportunityCardButton(
                resource = Res.drawable.open_window_16px,
                isAlwaysVisible = false,
                tooltip = "View In-Game",
                action = onViewInGameClick,
            ),
        )
        if (project.details.expires != null) {
            add(
                RiftOpportunityCardButton(
                    resource = Res.drawable.corporation_project_state_time_16px,
                    tooltipContent = {
                        val expiresIn = Duration.between(getNow(), project.details.expires).coerceAtLeast(Duration.ZERO)
                        Text(
                            text = buildAnnotatedString {
                                append("Expires in ")
                                withColor(RiftTheme.colors.textPrimary) {
                                    append(formatDuration(expiresIn))
                                }
                            },
                            style = RiftTheme.typography.bodySecondary,
                            modifier = Modifier.padding(Spacing.large),
                        )
                    },
                    action = null,
                ),
            )
        }
    }

    RiftOpportunityCard(
        category = category,
        type = type,
        solarSystemChipState = project.details.solarSystemChipState,
        topRight = progressGauge,
        bottomContent = bottomContent,
        buttons = buttons,
        isEnforcingHeight = true,
        onClick = onProjectClick,
        modifier = modifier,
    ) {
        Text(
            text = project.name,
            style = RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(top = Spacing.mediumLarge),
        )
    }
}

@Composable
private fun getProjectBottomContent(project: Project): RiftOpportunityCardBottomContent = when (project.state) {
    CorporationProjectState.Active, CorporationProjectState.Unspecified -> {
        if (project.reward != null) {
            val totalRemainingRewardForCharacters = if (project.details.rewardPerContribution != null && project.details.participationLimit != null) {
                val remainingRewardPerCharacter = project.eligibleCharacters.map { character ->
                    val characterContribution = project.contributions.find { it.characterId == character.characterId }
                    val characterProgress = (characterContribution?.contribution?.success ?: 0).coerceAtMost(project.details.participationLimit)
                    val characterRemainingProgress = (project.details.participationLimit - characterProgress).coerceIn(0, project.details.participationLimit)
                    val characterRemainingReward = project.details.rewardPerContribution * characterRemainingProgress
                    character to characterRemainingReward
                }
                remainingRewardPerCharacter.sumOf { it.second }.coerceAtMost(project.reward.remaining)
            } else {
                null
            }

            val bottomText = buildAnnotatedString {
                if (totalRemainingRewardForCharacters != null) {
                    append(formatIsk(totalRemainingRewardForCharacters))
                    append(" ")
                    withColor(RiftTheme.colors.textSecondary) {
                        append("(${formatNumberCompact(project.reward.remaining)})")
                    }
                } else {
                    append(formatIsk(project.reward.remaining))
                }
            }
            val bottomTextTooltip = buildAnnotatedString {
                if (totalRemainingRewardForCharacters != null) {
                    withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                        appendLine("Available to you")
                    }
                    withStyle(RiftTheme.typography.headerPrimary.toSpanStyle()) {
                        appendLine(formatIsk(totalRemainingRewardForCharacters))
                    }
                    withStyle(RiftTheme.typography.detailDisabled.toSpanStyle()) {
                        appendLine(formatIskReadable(totalRemainingRewardForCharacters))
                    }
                    appendLine()
                }

                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    appendLine("Available in Project")
                }
                withStyle(RiftTheme.typography.headerPrimary.toSpanStyle()) {
                    appendLine(formatIsk(project.reward.remaining))
                }
                withStyle(RiftTheme.typography.detailDisabled.toSpanStyle()) {
                    append(formatIskReadable(project.reward.remaining))
                }
            }
            RiftOpportunityCardBottomContent.Text(Res.drawable.isk, bottomText, bottomTextTooltip)
        } else {
            RiftOpportunityCardBottomContent.None
        }
    }

    CorporationProjectState.Closed -> {
        getProjectBottomText(
            text = "Closed",
            color = EveColors.hotRed,
            icon = Res.drawable.corporation_project_state_close_16px,
            timestamp = project.details.finished ?: project.lastModified,
        )
    }

    CorporationProjectState.Completed -> {
        getProjectBottomText(
            text = "Completed",
            color = EveColors.successGreen,
            icon = Res.drawable.corporation_project_state_checkmark_16px,
            timestamp = project.details.finished ?: project.lastModified,
        )
    }

    CorporationProjectState.Expired -> {
        getProjectBottomText(
            text = "Expired",
            color = EveColors.hotRed,
            icon = Res.drawable.corporation_project_state_time_16px,
            timestamp = project.details.finished ?: project.lastModified,
        )
    }

    CorporationProjectState.Deleted -> {
        getProjectBottomText(
            text = "Deleted",
            color = EveColors.hotRed,
            icon = Res.drawable.corporation_project_state_close_16px,
            timestamp = project.details.finished ?: project.lastModified,
        )
    }
}

@Composable
private fun getProjectBottomText(
    text: String,
    color: Color,
    icon: DrawableResource,
    timestamp: Instant?,
): RiftOpportunityCardBottomContent.Text {
    val bottomText = buildAnnotatedString {
        withColor(color) {
            append(text)
        }
        timestamp?.let {
            withColor(RiftTheme.colors.textSecondary) {
                append(" ")
                append(formatDateTime(it))
            }
        }
    }
    return RiftOpportunityCardBottomContent.Text(icon, bottomText)
}

@Composable
private fun EmptyState(text: String) {
    Text(
        text = text,
        style = RiftTheme.typography.headlineSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}
