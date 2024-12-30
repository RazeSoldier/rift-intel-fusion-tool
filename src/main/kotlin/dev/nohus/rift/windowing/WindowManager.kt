package dev.nohus.rift.windowing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement.Maximized
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import dev.nohus.rift.Event
import dev.nohus.rift.about.AboutWindow
import dev.nohus.rift.alerts.list.AlertsWindow
import dev.nohus.rift.assets.AssetsWindow
import dev.nohus.rift.characters.CharactersWindow
import dev.nohus.rift.charactersettings.CharacterSettingsWindow
import dev.nohus.rift.compose.UiScaleController
import dev.nohus.rift.configurationpack.ConfigurationPackReminderWindow
import dev.nohus.rift.contacts.ContactsWindow
import dev.nohus.rift.debug.DebugWindow
import dev.nohus.rift.fleet.FleetsWindow
import dev.nohus.rift.intel.feed.IntelFeedWindow
import dev.nohus.rift.intel.feed.settings.IntelFeedSettingsWindow
import dev.nohus.rift.intel.reports.IntelReportsWindow
import dev.nohus.rift.intel.reports.settings.IntelReportsSettingsWindow
import dev.nohus.rift.jabber.JabberInputModel
import dev.nohus.rift.jabber.JabberWindow
import dev.nohus.rift.map.MapWindow
import dev.nohus.rift.map.settings.MapSettingsWindow
import dev.nohus.rift.neocom.NeocomWindow
import dev.nohus.rift.pings.PingsWindow
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryWindow
import dev.nohus.rift.push.PushWindow
import dev.nohus.rift.settings.SettingsInputModel
import dev.nohus.rift.settings.SettingsWindow
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.settings.persistence.WindowSettings
import dev.nohus.rift.startupwarning.StartupWarningInputModel
import dev.nohus.rift.startupwarning.StartupWarningWindow
import dev.nohus.rift.utils.Pos
import dev.nohus.rift.utils.Size
import dev.nohus.rift.whatsnew.WhatsNewWindow
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.skiko.MainUIDispatcher
import org.koin.core.annotation.Single
import java.time.Instant
import java.util.UUID

@Single
class WindowManager(
    private val settings: Settings,
    private val uiScaleController: UiScaleController,
) {

    @Serializable
    enum class RiftWindow {
        @SerialName("Neocom")
        Neocom,

        @SerialName("Intel")
        IntelReports,

        @SerialName("IntelSettings")
        IntelReportsSettings,

        @SerialName("IntelFeed")
        IntelFeed,

        @SerialName("IntelFeedSettings")
        IntelFeedSettings,

        @SerialName("Settings")
        Settings,

        @SerialName("Map")
        Map,

        @SerialName("MapSettings")
        MapSettings,

        @SerialName("Characters")
        Characters,

        @SerialName("Pings")
        Pings,

        @SerialName("Alerts")
        Alerts,

        @SerialName("About")
        About,

        @SerialName("Jabber")
        Jabber,

        @SerialName("ConfigurationPackReminder")
        ConfigurationPackReminder,

        @SerialName("Assets")
        Assets,

        @SerialName("WhatsNew")
        WhatsNew,

        @SerialName("Debug")
        Debug,

        @SerialName("Fleets")
        Fleets,

        @SerialName("PlanetaryIndustry")
        PlanetaryIndustry,

        @SerialName("StartupWarning")
        StartupWarning,

        @SerialName("Push")
        Push,

        @SerialName("Contacts")
        Contacts,

        @SerialName("CharacterSettings")
        CharacterSettings,

        @Deprecated("Removed")
        @SerialName("NonEnglishEveClientWarning")
        NonEnglishEveClientWarning,

        @Deprecated("Removed")
        @SerialName("Pushover")
        Pushover,
    }

    data class RiftWindowState(
        val uuid: UUID = UUID.randomUUID(),
        val window: RiftWindow? = null,
        val inputModel: Any? = null,
        val windowState: WindowState,
        val minimumSize: Pair<Int?, Int?>,
        val openTimestamp: Instant = Instant.now(),
        val bringToFrontEvent: Event? = null,
    )

    private data class WindowInfo(
        val uuid: UUID,
        val geometry: WindowGeometry,
    )

    private data class WindowGeometry(
        val sizing: WindowSizing,
        val position: WindowPosition,
    )

    private data class WindowSizing(
        val defaultSize: Pair<Int?, Int?>,
        val minimumSize: Pair<Int?, Int?>,
    )

    private val nonSavedWindows = listOf(
        RiftWindow.IntelReportsSettings,
        RiftWindow.IntelFeedSettings,
        RiftWindow.MapSettings,
        RiftWindow.About,
        RiftWindow.ConfigurationPackReminder,
        RiftWindow.WhatsNew,
        RiftWindow.StartupWarning,
        RiftWindow.Pushover,
        RiftWindow.CharacterSettings,
    )
    private val multiInstanceWindows = listOf(
        RiftWindow.Map,
    )
    private val scope = CoroutineScope(Job())
    private var states: MutableState<Map<RiftWindow, List<RiftWindowState>>> = mutableStateOf(emptyMap())
    private val _windowEvents = MutableSharedFlow<WindowEvent>()
    val windowEvents = _windowEvents.asSharedFlow()

    sealed interface WindowEvent {
        data class WindowClosed(val window: RiftWindow, val uuid: UUID) : WindowEvent
    }

    fun openInitialWindows() {
        if (settings.isSetupWizardFinished) {
            if (settings.isRememberOpenWindows) {
                settings.openWindows.filter { it !in nonSavedWindows }.forEach { onWindowOpen(it) }
            }
            if (!settings.isRememberOpenWindows || !settings.isTrayIconWorking) {
                onWindowOpen(RiftWindow.Neocom, ifClosed = true)
            }
        }
    }

    suspend fun saveWindowPlacements() = withContext(MainUIDispatcher) {
        rememberWindowPlacements()
    }

    @Composable
    fun composeWindows() {
        for ((window, states) in states.value) {
            for (state in states) {
                key(state.uuid) {
                    CompositionLocalProvider(LocalRiftWindowState provides state) {
                        when (window) {
                            RiftWindow.Neocom -> NeocomWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Neocom, state.uuid) })
                            RiftWindow.IntelReports -> IntelReportsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.IntelReports, state.uuid) }, onTuneClick = { onWindowOpen(RiftWindow.IntelReportsSettings) })
                            RiftWindow.IntelReportsSettings -> IntelReportsSettingsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.IntelReportsSettings, state.uuid) })
                            RiftWindow.IntelFeed -> IntelFeedWindow(state, onCloseRequest = { onWindowClose(RiftWindow.IntelFeed, state.uuid) }, onTuneClick = { onWindowOpen(RiftWindow.IntelFeedSettings) })
                            RiftWindow.IntelFeedSettings -> IntelFeedSettingsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.IntelFeedSettings, state.uuid) })
                            RiftWindow.Settings -> SettingsWindow(state.inputModel as? SettingsInputModel ?: SettingsInputModel.Normal, state, onCloseRequest = { onWindowClose(RiftWindow.Settings, state.uuid) })
                            RiftWindow.Map -> MapWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Map, state.uuid) }, onTuneClick = { onWindowOpen(RiftWindow.MapSettings) })
                            RiftWindow.MapSettings -> MapSettingsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.MapSettings, state.uuid) })
                            RiftWindow.Characters -> CharactersWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Characters, state.uuid) })
                            RiftWindow.Alerts -> AlertsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Alerts, state.uuid) })
                            RiftWindow.About -> AboutWindow(state, onCloseRequest = { onWindowClose(RiftWindow.About, state.uuid) })
                            RiftWindow.Jabber -> JabberWindow(state.inputModel as? JabberInputModel ?: JabberInputModel.None, state, onCloseRequest = { onWindowClose(RiftWindow.Jabber, state.uuid) })
                            RiftWindow.Pings -> PingsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Pings, state.uuid) })
                            RiftWindow.ConfigurationPackReminder -> ConfigurationPackReminderWindow(state, onCloseRequest = { onWindowClose(RiftWindow.ConfigurationPackReminder, state.uuid) })
                            RiftWindow.Assets -> AssetsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Assets, state.uuid) })
                            RiftWindow.WhatsNew -> WhatsNewWindow(state, onCloseRequest = { onWindowClose(RiftWindow.WhatsNew, state.uuid) })
                            RiftWindow.Debug -> DebugWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Debug, state.uuid) })
                            RiftWindow.Fleets -> FleetsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Fleets, state.uuid) })
                            RiftWindow.PlanetaryIndustry -> PlanetaryIndustryWindow(state, onCloseRequest = { onWindowClose(RiftWindow.PlanetaryIndustry, state.uuid) })
                            RiftWindow.StartupWarning -> StartupWarningWindow(state.inputModel as? StartupWarningInputModel, state, onCloseRequest = { onWindowClose(RiftWindow.StartupWarning, state.uuid) })
                            RiftWindow.Push -> PushWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Push, state.uuid) })
                            RiftWindow.Contacts -> ContactsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Contacts, state.uuid) })
                            RiftWindow.CharacterSettings -> CharacterSettingsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.CharacterSettings, state.uuid) })
                            RiftWindow.NonEnglishEveClientWarning -> {}
                            RiftWindow.Pushover -> {}
                        }
                    }
                }
            }
        }
    }

    fun onWindowOpen(window: RiftWindow, inputModel: Any? = null, ifClosed: Boolean = false) {
        val existingStates = states.value[window] ?: emptyList()
        if (ifClosed && existingStates.isNotEmpty()) return
        val newStates = if (existingStates.isNotEmpty()) {
            // Already open
            if (window in multiInstanceWindows) {
                // This is a multi instance window, open another instance
                if (existingStates.size >= 5) return // Don't open more than 5 instances of a window
                val geometry = getWindowOpenInfo(window, forceNew = true).single()
                existingStates + createWindowState(window, geometry, inputModel)
            } else {
                // This is a single instance window, bring up the existing window
                existingStates.map {
                    it.copy(
                        inputModel = inputModel,
                        openTimestamp = Instant.now(),
                        bringToFrontEvent = Event(),
                    )
                }
            }
        } else {
            // There is no existing window, restore or create a new one
            createWindowStates(window, inputModel)
        }
        settings.openWindows += window
        states.value += window to newStates
    }

    private fun createWindowStates(window: RiftWindow, inputModel: Any?): List<RiftWindowState> {
        val geometries = getWindowOpenInfo(window, forceNew = false)
        return geometries.mapIndexed { index, geometry ->
            val instanceInputModel = if (window in multiInstanceWindows) {
                // This is a multi instance window, only use the input model on the latest window
                if (index == geometries.lastIndex) inputModel else null
            } else {
                // This is a single instance window, use the input model as provided
                inputModel
            }
            createWindowState(window, geometry, instanceInputModel)
        }
    }

    private fun createWindowState(window: RiftWindow, info: WindowInfo, inputModel: Any?): RiftWindowState {
        val geometry = info.geometry
        val state = WindowState(
            width = geometry.sizing.defaultSize.first?.dp ?: Dp.Unspecified,
            height = geometry.sizing.defaultSize.second?.dp ?: Dp.Unspecified,
            position = geometry.position,
        )
        return RiftWindowState(
            uuid = info.uuid,
            window = window,
            inputModel = inputModel,
            windowState = state,
            minimumSize = geometry.sizing.minimumSize,
        )
    }

    fun onWindowClose(window: RiftWindow, uuid: UUID?) {
        val existingStates = states.value[window] ?: return
        val statesToClose = if (uuid != null) {
            listOfNotNull(existingStates.firstOrNull { it.uuid == uuid })
        } else {
            existingStates
        }
        if (statesToClose.size == existingStates.size) {
            // Closing all windows
            states.value -= window
            settings.openWindows -= window
        } else {
            // Closing some windows
            states.value += window to (existingStates - statesToClose)
        }

        statesToClose.forEach {
            scope.launch {
                _windowEvents.emit(WindowEvent.WindowClosed(window, it.uuid))
            }
        }
    }

    /**
     * Returns the geometries for opening new instances of a given RiftWindow.
     * This will be the saved geometries of potentially multiple saved instances of this window,
     * or a single new geometry if there are no saved instances or saving is not used.
     *
     * @param forceNew When true, will return a single new window geometry
     */
    private fun getWindowOpenInfo(window: RiftWindow, forceNew: Boolean): List<WindowInfo> {
        val savedPlacements = if (settings.isRememberWindowPlacement && !forceNew) {
            settings.windowSettings[window] ?: listOf(null)
        } else {
            listOf(null)
        }
        return savedPlacements.map { saved ->
            val sizing = getWindowOpenSizing(window, saved)
            val position = getWindowOpenPosition(saved)
            val geometry = WindowGeometry(sizing, position)
            WindowInfo(
                uuid = saved?.uuid ?: UUID.randomUUID(),
                geometry = geometry,
            )
        }
    }

    private fun getWindowOpenSizing(window: RiftWindow, savedPlacement: WindowSettings?): WindowSizing {
        val saved = savedPlacement?.size?.let { it.width to it.height }
        val windowSizing = when (window) {
            RiftWindow.Neocom -> WindowSizing(defaultSize = saved ?: (160 to 650), minimumSize = 143 to 106)
            RiftWindow.IntelReports -> WindowSizing(defaultSize = saved ?: (800 to 500), minimumSize = 400 to 200)
            RiftWindow.IntelReportsSettings -> WindowSizing(defaultSize = (400 to null), minimumSize = 400 to null)
            RiftWindow.IntelFeed -> WindowSizing(defaultSize = saved ?: (500 to 600), minimumSize = (500 to 250))
            RiftWindow.IntelFeedSettings -> WindowSizing(defaultSize = (400 to null), minimumSize = 400 to null)
            RiftWindow.Settings -> WindowSizing(defaultSize = (820 to null), minimumSize = 820 to null)
            RiftWindow.Map -> WindowSizing(defaultSize = saved ?: (800 to 800), minimumSize = 350 to 300)
            RiftWindow.MapSettings -> WindowSizing(defaultSize = (400 to 450), minimumSize = 400 to 450)
            RiftWindow.Characters -> WindowSizing(defaultSize = saved ?: (420 to 400), minimumSize = 400 to 300)
            RiftWindow.Alerts -> WindowSizing(defaultSize = saved ?: (500 to 500), minimumSize = 500 to 500)
            RiftWindow.About -> WindowSizing(defaultSize = (500 to null), minimumSize = (500 to null))
            RiftWindow.Jabber -> WindowSizing(defaultSize = saved ?: (400 to 500), minimumSize = (200 to 200))
            RiftWindow.Pings -> WindowSizing(defaultSize = saved ?: (440 to 500), minimumSize = (440 to 300))
            RiftWindow.ConfigurationPackReminder -> WindowSizing(defaultSize = (450 to null), minimumSize = (450 to null))
            RiftWindow.Assets -> WindowSizing(defaultSize = saved ?: (500 to 500), minimumSize = (500 to 300))
            RiftWindow.WhatsNew -> WindowSizing(defaultSize = (450 to 600), minimumSize = (450 to 600))
            RiftWindow.Debug -> WindowSizing(defaultSize = saved ?: (450 to 500), minimumSize = (450 to 500))
            RiftWindow.Fleets -> WindowSizing(defaultSize = saved ?: (300 to 300), minimumSize = 300 to 300)
            RiftWindow.PlanetaryIndustry -> WindowSizing(defaultSize = saved ?: (540 to 800), minimumSize = 540 to 360)
            RiftWindow.StartupWarning -> WindowSizing(defaultSize = (450 to null), minimumSize = (450 to null))
            RiftWindow.Push -> WindowSizing(defaultSize = (350 to 435), minimumSize = 350 to 435)
            RiftWindow.Contacts -> WindowSizing(defaultSize = saved ?: (650 to 600), minimumSize = 650 to 600)
            RiftWindow.CharacterSettings -> WindowSizing(defaultSize = (420 to 500), minimumSize = 400 to 300)
            RiftWindow.NonEnglishEveClientWarning -> WindowSizing(defaultSize = (200 to 200), minimumSize = (200 to 200))
            RiftWindow.Pushover -> WindowSizing(defaultSize = (200 to 200), minimumSize = (200 to 200))
        }
        return windowSizing.scaled(uiScaleController.uiScale)
    }

    private fun WindowSizing.scaled(scale: Float): WindowSizing {
        return WindowSizing(
            defaultSize = defaultSize.first?.let { (it * scale).toInt() } to defaultSize.second?.let { (it * scale).toInt() },
            minimumSize = minimumSize.first?.let { (it * scale).toInt() } to minimumSize.second?.let { (it * scale).toInt() },
        )
    }

    private fun getWindowOpenPosition(savedPlacement: WindowSettings?): WindowPosition {
        val saved = savedPlacement?.position ?: return WindowPosition.PlatformDefault
        return WindowPosition(saved.x.dp, saved.y.dp)
    }

    private fun rememberWindowPlacements() {
        val scale = uiScaleController.uiScale
        states.value
            .flatMap { (window, states) -> states.map { window to it } }
            .filter { (_, state) -> state.window !in nonSavedWindows }
            .filter { (_, state) -> state.windowState.placement != Maximized }
            .groupBy(keySelector = { (window, _) -> window }, valueTransform = { (_, states) -> states })
            .forEach { (window, states) ->
                val existingSettings = settings.windowSettings[window] ?: emptyList()
                val newSettings = states.map { state ->
                    val position = state.windowState.position.let { Pos(it.x.value.toInt(), it.y.value.toInt()) }
                    val size = state.windowState.size.let { Size((it.width.value / scale).toInt(), (it.height.value / scale).toInt()) }
                    val settings = existingSettings.firstOrNull { it.uuid == state.uuid }
                    settings?.copy(
                        position = position,
                        size = size,
                    ) ?: WindowSettings(
                        uuid = state.uuid,
                        position = position,
                        size = size,
                    )
                }
                settings.windowSettings += window to newSettings
            }
    }

    fun getOpenWindowUuids(window: RiftWindow): List<UUID> {
        return states.value[window]?.map { it.uuid } ?: emptyList()
    }
}

val LocalRiftWindowState: ProvidableCompositionLocal<RiftWindowState?> = staticCompositionLocalOf { null }
val LocalRiftWindow: ProvidableCompositionLocal<ComposeWindow?> = staticCompositionLocalOf { null }
