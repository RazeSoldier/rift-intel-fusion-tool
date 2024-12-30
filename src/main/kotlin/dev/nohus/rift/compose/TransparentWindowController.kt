package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.LocalWindowInfo
import dev.nohus.rift.utils.OperatingSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.awt.Component
import java.awt.peer.WindowPeer

private val logger = KotlinLogging.logger {}

@Single
class TransparentWindowController(
    private val operatingSystem: OperatingSystem,
) {

    /**
     * Compose-level window transparency is disabled on Linux
     */
    fun isComposeWindowTransparent(): Boolean {
        return operatingSystem != OperatingSystem.Linux
    }

    /**
     * System-level window transparency is used on Linux
     */
    @Composable
    fun setTransparency(window: ComposeWindow, enabled: Boolean) {
        if (operatingSystem == OperatingSystem.Linux) {
            val isActive = LocalWindowInfo.current.isWindowFocused
            remember(window, enabled, isActive) {
                setLinuxTransparency(window, enabled, isActive)
            }
        }
    }

    @Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
    private fun setLinuxTransparency(window: ComposeWindow, enabled: Boolean, isActive: Boolean) {
        try {
            val peerField = Component::class.java.getDeclaredField("peer")
            peerField.setAccessible(true)
            val peer = peerField.get(window)
            (peer as WindowPeer).setOpacity(getOpacity(enabled, isActive))
        } catch (e: ReflectiveOperationException) {
            logger.error { "Could not set window opacity: $e" }
        }
    }

    private fun getOpacity(enabled: Boolean, isActive: Boolean): Float {
        return when {
            enabled -> if (isActive) 0.8f else 0.55f
            else -> 1f
        }
    }
}
