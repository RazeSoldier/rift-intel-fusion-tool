package dev.nohus.rift

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.application
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.crash.RiftExceptionHandlerFactory
import dev.nohus.rift.crash.handleFatalException
import dev.nohus.rift.di.koin
import dev.nohus.rift.di.startKoin
import dev.nohus.rift.i18n.ApplicationLocale
import dev.nohus.rift.logging.initializeLogging
import dev.nohus.rift.notifications.NotificationsController
import dev.nohus.rift.singleinstance.SingleInstanceWrapper
import dev.nohus.rift.splash.SplashWindowWrapper
import dev.nohus.rift.tray.RiftTray
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.wizard.WizardWindowWrapper
import io.kamel.image.config.LocalKamelConfig
import java.util.*

fun main() {
    try {
        initializeLogging()
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

        Locale.setDefault(ApplicationLocale.current)
        RiftTheme {
            if (state.isAnotherInstanceDialogShown) {
                SingleInstanceWrapper(
                    onRunAnywayClick = viewModel::onSingleInstanceRunAnywayClick,
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
