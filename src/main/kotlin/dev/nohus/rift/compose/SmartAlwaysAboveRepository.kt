package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalWindowInfo
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.activewindow.ActiveEveWindowRepository
import dev.nohus.rift.windowing.LocalRiftWindowState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.util.UUID

/**
 * Controls whether "always above" property of windows should take effect
 */
@Single
class SmartAlwaysAboveRepository(
    private val activeEveWindowRepository: ActiveEveWindowRepository,
    private val settings: Settings,
) {

    private var focusedRiftWindow: UUID? = null
    private val _isActive = MutableStateFlow(false)
    val isActive = _isActive.asStateFlow()

    suspend fun start() = coroutineScope {
        launch {
            activeEveWindowRepository.activeWindowCharacter.map { it != null }.collect {
                updateState()
            }
        }
    }

    @Composable
    fun registerWindow() {
        val isFocused = LocalWindowInfo.current.isWindowFocused
        val windowUuid = LocalRiftWindowState.current?.uuid ?: return
        LaunchedEffect(isFocused, windowUuid) {
            if (isFocused) {
                focusedRiftWindow = windowUuid
                updateState()
            } else {
                if (focusedRiftWindow == windowUuid) {
                    focusedRiftWindow = null
                    if (settings.isSmartAlwaysAbove) {
                        activeEveWindowRepository.checkNow()
                    }
                    updateState()
                }
            }
        }
    }

    private fun updateState() {
        _isActive.value = !settings.isSmartAlwaysAbove ||
            focusedRiftWindow != null ||
            activeEveWindowRepository.activeWindowCharacter.value != null
    }
}
