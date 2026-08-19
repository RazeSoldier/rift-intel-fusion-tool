package dev.nohus.rift.postkillmail

import dev.nohus.rift.ViewModel
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.windowing.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
class PostKillmailViewModel(
    @InjectedParam private val inputModel: PostKillmailInputModel,
    private val postKillmailUseCase: PostKillmailUseCase,
    private val settings: Settings,
    private val windowManager: WindowManager,
) : ViewModel() {

    data class UiState(
        val selectedDelay: KillmailPostingDelay,
        val isPosting: Boolean = false,
    )

    private val _state = MutableStateFlow(
        UiState(
            selectedDelay = KillmailPostingDelay.fromValue(settings.killmailPostingDelay),
        ),
    )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.updateFlow.map { it.killmailPostingDelay }.collect { delay ->
                _state.update { it.copy(selectedDelay = KillmailPostingDelay.fromValue(delay)) }
            }
        }
    }

    fun onDelaySelected(delay: KillmailPostingDelay) {
        settings.killmailPostingDelay = delay.value
        _state.update { it.copy(selectedDelay = delay) }
    }

    fun onPostClick() {
        if (_state.value.isPosting) return
        viewModelScope.launch {
            _state.update { it.copy(isPosting = true) }
            postKillmailUseCase(inputModel.killId, inputModel.hash, _state.value.selectedDelay)
            windowManager.onWindowClose(WindowManager.RiftWindow.PostKillmail, uuid = null)
        }
    }
}
