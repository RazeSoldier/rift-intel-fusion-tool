package dev.nohus.rift.corpprojects

import dev.nohus.rift.ViewModel
import dev.nohus.rift.corpprojects.CorporationProjectsRepository.CorporationProjects
import dev.nohus.rift.corpprojects.CorporationProjectsRepository.LoadingState
import dev.nohus.rift.game.GameUiController
import dev.nohus.rift.network.esi.models.CorporationProjectState
import dev.nohus.rift.utils.sumOfDouble
import dev.nohus.rift.utils.toggle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import java.time.Duration
import java.time.Instant

@Factory
class CorporationProjectsViewModel(
    private val corporationProjectsRepository: CorporationProjectsRepository,
    private val gameUiController: GameUiController,
) : ViewModel() {

    data class UiState(
        val projects: CorporationProjects? = null,
        val corporations: List<Corporation> = emptyList(),
        val view: View = View.ProjectsView,
        val projectsStats: ProjectsStats? = null,
        val lifecycleFilter: ProjectLifecycleFilter = ProjectLifecycleFilter.Active,
        val applicableCategoryFilters: Set<ProjectCategoryFilter> = emptySet(),
        val enabledCategoryFilters: Set<ProjectCategoryFilter> = emptySet(),
        val sorting: ProjectSorting = ProjectSorting.Name,
        val search: String? = null,
        val loading: LoadingState,
    )

    data class ProjectsStats(
        val availableToYou: Double,
        val availableTotal: Double,
        val active: Int,
        val completedToday: Int,
        val completedThisWeek: Int,
    )

    sealed interface View {
        data object ProjectsView : View
        data class DetailsView(val project: Project) : View
    }

    enum class ProjectLifecycleFilter {
        Active,
        History,
    }

    enum class ProjectSorting {
        Name,
        NameReversed,
        DateCreated,
        DateCreatedReversed,
        TimeRemaining,
        TimeRemainingReversed,
        Progress,
        ProgressReversed,
        NumberOfJumps,
        NumberOfJumpsReversed,
    }

    private val _state = MutableStateFlow(
        UiState(
            loading = corporationProjectsRepository.projects.value.loading,
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            corporationProjectsRepository.projects.collect { projects ->
                _state.update { it.copy(loading = projects.loading) }
                updateProjects()
            }
        }
    }

    fun onVisibilityChange(visible: Boolean) {
        viewModelScope.launch {
            corporationProjectsRepository.setNeedsRealtimeUpdates(visible)
            if (visible) {
                corporationProjectsRepository.reload()
            }
        }
    }

    fun onLifecycleFilterChange(filter: ProjectLifecycleFilter) {
        _state.update { it.copy(lifecycleFilter = filter) }
        updateProjects()
    }

    fun onCategoryFilterChange(filter: ProjectCategoryFilter) {
        when (_state.value.view) {
            is View.DetailsView -> {
                _state.update {
                    it.copy(
                        view = View.ProjectsView,
                        enabledCategoryFilters = if (filter !in it.enabledCategoryFilters) {
                            it.enabledCategoryFilters + filter
                        } else {
                            it.enabledCategoryFilters
                        },
                    )
                }
            }
            View.ProjectsView -> {
                _state.update {
                    it.copy(enabledCategoryFilters = it.enabledCategoryFilters.toggle(filter))
                }
            }
        }
        updateProjects()
    }

    fun onSortingChange(sorting: ProjectSorting) {
        _state.update { it.copy(sorting = sorting) }
        updateProjects()
    }

    fun onSearchChange(text: String) {
        val search = text.takeIf { it.isNotBlank() }?.trim()
        _state.update { it.copy(search = search) }
        updateProjects()
    }

    fun onCorporationSelect(corporation: Corporation) {
        updateProjects(corporation)
    }

    fun onProjectClick(project: Project) {
        _state.update { it.copy(view = View.DetailsView(project)) }
    }

    fun onViewInGameClick(project: Project) {
        gameUiController.pushCorporationProject(project.id, project.name)
    }

    fun onBackClick() {
        _state.update { it.copy(view = View.ProjectsView) }
    }

    private fun updateProjects(corporation: Corporation? = null) {
        val projects = corporationProjectsRepository.projects.value
        val corporations = projects.corporationProjects.map { it.corporation }
        val selectedCorporation = (corporation ?: _state.value.projects?.corporation)
            ?.takeIf { it in corporations }
            ?: corporations.firstOrNull()
        _state.update {
            it.copy(
                corporations = corporations,
            )
        }

        val corporationProjects = projects.corporationProjects
            .firstOrNull { it.corporation == selectedCorporation }
        if (corporationProjects != null) {
            val filteredProjects = getFilteredProjects(corporationProjects.projects)
            val projectsStats = getProjectsStats(corporationProjects.projects)
            val applicableCategoryFilters = getApplicableCategoryFilters(filteredProjects)
            val enabledCategoryFilters = _state.value.enabledCategoryFilters.intersect(applicableCategoryFilters)
            _state.update {
                it.copy(
                    projects = corporationProjects.copy(projects = filteredProjects),
                    projectsStats = projectsStats,
                    applicableCategoryFilters = applicableCategoryFilters,
                    enabledCategoryFilters = enabledCategoryFilters,
                )
            }

            // Update the detail view if it's open
            val viewedProjectId = (_state.value.view as? View.DetailsView)?.project?.id
            if (viewedProjectId != null) {
                val allProjects = projects.corporationProjects.flatMap { it.projects }
                val updatedViewedProject = allProjects.firstOrNull { it.id == viewedProjectId }
                if (updatedViewedProject != null) {
                    _state.update { it.copy(view = View.DetailsView(updatedViewedProject)) }
                }
            }
        } else {
            _state.update {
                it.copy(
                    projects = null,
                    projectsStats = null,
                    applicableCategoryFilters = emptySet(),
                    enabledCategoryFilters = emptySet(),
                )
            }
        }
    }

    private fun getProjectsStats(projects: List<Project>): ProjectsStats {
        val now = Instant.now()
        val todayCutoff = now - Duration.ofDays(1)
        val weekCutoff = now - Duration.ofDays(7)
        val activeProjects = projects.filter { it.state == CorporationProjectState.Active }
        return ProjectsStats(
            availableToYou = activeProjects.sumOfDouble {
                val maxContributionPerCharacter = it.details.participationLimit ?: it.desiredProgress
                val availableContribution = it.contributions.sumOf {
                    val contribution = (it.contribution.success ?: 0L).coerceAtMost(maxContributionPerCharacter)
                    maxContributionPerCharacter - contribution
                }.coerceAtMost(it.desiredProgress)
                (it.details.rewardPerContribution?.times(availableContribution) ?: 0.0).coerceAtMost(it.reward?.remaining ?: 0.0)
            },
            availableTotal = activeProjects.sumOfDouble {
                it.reward?.remaining ?: 0.0
            },
            active = activeProjects.size,
            completedToday = projects.count {
                it.state == CorporationProjectState.Completed && it.details.finished?.isAfter(todayCutoff) == true
            },
            completedThisWeek = projects.count {
                it.state == CorporationProjectState.Completed && it.details.finished?.isAfter(weekCutoff) == true
            },
        )
    }

    private fun getFilteredProjects(projects: List<Project>): List<Project> {
        val filteredProjects = projects
            .filter {
                when (state.value.lifecycleFilter) {
                    ProjectLifecycleFilter.Active -> it.state == CorporationProjectState.Active
                    ProjectLifecycleFilter.History -> it.state != CorporationProjectState.Active
                }
            }
            .filter {
                state.value.search?.let { search ->
                    it.name.contains(search, ignoreCase = true)
                } ?: true
            }
            .filter { project ->
                val enabledFilters = state.value.enabledCategoryFilters
                if (enabledFilters.isEmpty()) return@filter true
                val matchingFilters = project.details.matchingFilters
                enabledFilters.all { it in matchingFilters }
            }

        val comparator = when (state.value.sorting) {
            ProjectSorting.Name -> compareBy<Project> { it.name.lowercase() }
            ProjectSorting.NameReversed -> compareByDescending { it.name.lowercase() }
            ProjectSorting.DateCreated -> compareBy { it.details.created }
            ProjectSorting.DateCreatedReversed -> compareByDescending { it.details.created }
            ProjectSorting.TimeRemaining -> compareBy { it.details.expires ?: Instant.MAX }
            ProjectSorting.TimeRemainingReversed -> compareByDescending { it.details.expires ?: Instant.MAX }
            ProjectSorting.Progress -> compareBy { it.currentProgress }
            ProjectSorting.ProgressReversed -> compareByDescending { it.currentProgress }
            ProjectSorting.NumberOfJumps -> compareBy { it.details.solarSystemChipState?.distance?.toFloat() ?: 0.5f }
            ProjectSorting.NumberOfJumpsReversed -> compareByDescending { it.details.solarSystemChipState?.distance?.toFloat() ?: 0.5f }
        }.thenBy { it.id }
        val sortedProjects = filteredProjects.sortedWith(comparator)

        return sortedProjects
    }

    private fun getApplicableCategoryFilters(projects: List<Project>): Set<ProjectCategoryFilter> {
        return projects
            .flatMap { it.details.matchingFilters }
            .distinct()
            .sortedBy { it.order }
            .toCollection(LinkedHashSet())
    }

    fun onReloadClick() {
        viewModelScope.launch {
            corporationProjectsRepository.reload()
        }
    }
}
