package dev.nohus.rift

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.application
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.crash.RiftExceptionHandlerFactory
import dev.nohus.rift.crash.handleFatalException
import dev.nohus.rift.di.koin
import dev.nohus.rift.di.startKoin
import dev.nohus.rift.logging.initializeLogging
import dev.nohus.rift.notifications.NotificationsController
import dev.nohus.rift.singleinstance.SingleInstanceWrapper
import dev.nohus.rift.splash.SplashWindowWrapper
import dev.nohus.rift.tray.RiftTray
import dev.nohus.rift.utils.GetOperatingSystemUseCase
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.AppDirectories
import dev.nohus.rift.utils.osdirectories.LinuxDirectories
import dev.nohus.rift.utils.osdirectories.MacDirectories
import dev.nohus.rift.utils.osdirectories.WindowsDirectories
import dev.nohus.rift.whatsnew.MigrateAppDataUseCase
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.wizard.WizardWindowWrapper
import io.kamel.image.config.LocalKamelConfig

fun main() {
    try {
        initializeLogging()
        runMigration()
        startKoin()
        application {
            riftApplication()
        }
    } catch (e: Throwable) {
        handleFatalException(e)
        throw e
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ApplicationScope.riftApplication() {
    CompositionLocalProvider(
        LocalKamelConfig provides remember { koin.get() },
        LocalWindowExceptionHandlerFactory provides RiftExceptionHandlerFactory,
    ) {
        val viewModel: ApplicationViewModel = viewModel()
        val windowManager: WindowManager = remember { koin.get() }
        val notificationsController: NotificationsController = remember { koin.get() }
        val state by viewModel.state.collectAsState()

        RiftTheme {
            if (state.isAnotherInstanceDialogShown) {
                SingleInstanceWrapper(
                    onCloseRequest = viewModel::onQuit,
                )
            } else {
                RiftTray(state.isTrayIconShown)
                windowManager.composeWindows()
                SplashWindowWrapper(
                    isVisible = state.isSplashScreenShown,
                    onCloseRequest = {},
                )
                WizardWindowWrapper(
                    isVisible = state.isSetupWizardShown,
                    onCloseRequest = viewModel::onWizardCloseRequest,
                )
            }
            notificationsController.composeNotification()
        }

        if (!state.isApplicationRunning) exitApplication()
    }
}

private fun runMigration() {
    val operatingSystem = GetOperatingSystemUseCase()()
    val appDirectories = AppDirectories(
        when (operatingSystem) {
            OperatingSystem.Linux -> LinuxDirectories()
            OperatingSystem.Windows -> WindowsDirectories()
            OperatingSystem.MacOs -> MacDirectories()
        },
    )
    MigrateAppDataUseCase(operatingSystem, appDirectories)()
}
