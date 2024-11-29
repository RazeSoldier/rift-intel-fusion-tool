package dev.nohus.rift.neocom

import dev.nohus.rift.ApplicationViewModel
import dev.nohus.rift.ViewModel
import dev.nohus.rift.configurationpack.ConfigurationPackRepository
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single
class NeocomViewModel(
    private val windowManager: WindowManager,
    private val applicationViewModel: ApplicationViewModel,
    private val configurationPackRepository: ConfigurationPackRepository,
) : ViewModel() {

    data class UiState(
        val isJabberEnabled: Boolean = false,
    )

    private val _state = MutableStateFlow(
        UiState(
            isJabberEnabled = configurationPackRepository.isJabberEnabled(),
        ),
    )
    val state = _state.asStateFlow()

    fun onButtonClick(window: RiftWindow) {
        windowManager.onWindowOpen(window)
    }

    fun onQuitClick() {
        applicationViewModel.onQuit()
    }
}
