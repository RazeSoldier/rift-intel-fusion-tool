package dev.nohus.rift.assets

import dev.nohus.rift.DataEvent
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single
class AssetsExternalControl(
    private val windowManager: WindowManager,
) {

    private val _event = MutableStateFlow<DataEvent<AssetsExternalControlEvent>?>(null)
    val event = _event.asStateFlow()

    sealed interface AssetsExternalControlEvent {
        data class ShowSystem(val systemName: String) : AssetsExternalControlEvent
    }

    fun showSystem(systemName: String) {
        windowManager.onWindowOpen(RiftWindow.Assets, ifClosed = true)
        _event.tryEmit(DataEvent(AssetsExternalControlEvent.ShowSystem(systemName)))
    }
}
