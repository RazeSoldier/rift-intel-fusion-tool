package dev.nohus.rift.structures

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.opportunities.MercenaryTacticalOperationsRepository
import dev.nohus.rift.opportunities.MercenaryTacticalOperationsRepository.Operations
import dev.nohus.rift.opportunities.OpportunitiesInputModel
import dev.nohus.rift.structures.StructuresRepository.Structures
import dev.nohus.rift.windowing.WindowManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
@Factory
class StructuresViewModel(
    private val structuresRepository: StructuresRepository,
    private val mercenaryTacticalOperationsRepository: MercenaryTacticalOperationsRepository,
    private val windowManager: WindowManager,
) : ViewModel() {

    data class UiState(
        val selectedTab: StructuresTab = StructuresTab.Skyhooks,
        val structures: Structures? = null,
        val operations: Operations? = null,
    )

    enum class StructuresTab {
        Skyhooks,
        SovereigntyHubs,
        MercenaryDens,
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            structuresRepository.structures.collect { structures ->
                _state.update { it.copy(structures = structures) }
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
            structuresRepository.setNeedsRealtimeUpdates(visible)
        }
    }

    fun onTabSelected(tab: StructuresTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun onViewOperationClick(id: String) {
        windowManager.onWindowOpen(WindowManager.RiftWindow.Opportunities, OpportunitiesInputModel(id))
    }
}
