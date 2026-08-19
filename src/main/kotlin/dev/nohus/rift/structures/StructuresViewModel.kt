package dev.nohus.rift.structures

import dev.nohus.rift.ViewModel
import dev.nohus.rift.opportunities.MercenaryTacticalOperationsRepository
import dev.nohus.rift.opportunities.MercenaryTacticalOperationsRepository.Operations
import dev.nohus.rift.opportunities.OpportunitiesInputModel
import dev.nohus.rift.structures.EquinoxStructuresRepository.LoadingState
import dev.nohus.rift.structures.EquinoxStructuresRepository.Structures
import dev.nohus.rift.windowing.WindowManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

@OptIn(FlowPreview::class)
@Factory
class StructuresViewModel(
    private val equinoxStructuresRepository: EquinoxStructuresRepository,
    private val mercenaryTacticalOperationsRepository: MercenaryTacticalOperationsRepository,
    private val windowManager: WindowManager,
) : ViewModel() {

    data class UiState(
        val selectedTab: StructuresTab = StructuresTab.Skyhooks,
        val structures: Structures? = null,
        val loading: LoadingState = LoadingState(),
        val operations: Operations? = null,
        val skyhookResourceFilter: SkyhookResourceFilter = SkyhookResourceFilter.All,
    )

    sealed interface SkyhookResourceFilter {
        data object All : SkyhookResourceFilter
        data object Power : SkyhookResourceFilter
        data object Workforce : SkyhookResourceFilter
        data object ReagentGas : SkyhookResourceFilter
        data object ReagentIce : SkyhookResourceFilter
    }

    enum class StructuresTab {
        Skyhooks,
        SovereigntyHubs,
        MercenaryDens,
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            equinoxStructuresRepository.state.collect { state ->
                _state.update { it.copy(structures = state.structures, loading = state.loading) }
            }
        }
        viewModelScope.launch {
            mercenaryTacticalOperationsRepository.operations.collect { operations ->
                _state.update { it.copy(operations = operations) }
            }
        }
    }

    fun onVisibilityChange(visible: Boolean) {
        viewModelScope.launch {
            equinoxStructuresRepository.setNeedsRealtimeUpdates(visible)
        }
    }

    fun onTabSelected(tab: StructuresTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun onSkyhookResourceFilterSelected(filter: SkyhookResourceFilter) {
        _state.update { it.copy(skyhookResourceFilter = filter) }
    }

    fun onViewOperationClick(id: String) {
        windowManager.onWindowOpen(WindowManager.RiftWindow.Opportunities, OpportunitiesInputModel(id))
    }
}
