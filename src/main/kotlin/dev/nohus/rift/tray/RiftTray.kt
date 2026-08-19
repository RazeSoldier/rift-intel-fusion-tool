package dev.nohus.rift.tray

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.ApplicationScope
import com.kdroid.composetray.tray.api.Tray
import com.kdroid.composetray.utils.IconRenderProperties
import com.kdroid.composetray.utils.isMenuBarInDarkMode
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.tray_tray_128
import dev.nohus.rift.generated.resources.tray_tray_dark_128
import dev.nohus.rift.generated.resources.window_assets
import dev.nohus.rift.generated.resources.window_bleedchannel
import dev.nohus.rift.generated.resources.window_characters
import dev.nohus.rift.generated.resources.window_chatchannels
import dev.nohus.rift.generated.resources.window_contacts
import dev.nohus.rift.generated.resources.window_evemailtag
import dev.nohus.rift.generated.resources.window_jukebox
import dev.nohus.rift.generated.resources.window_loudspeaker_icon
import dev.nohus.rift.generated.resources.window_map
import dev.nohus.rift.generated.resources.window_opportunities
import dev.nohus.rift.generated.resources.window_planets
import dev.nohus.rift.generated.resources.window_quitgame
import dev.nohus.rift.generated.resources.window_rift_64
import dev.nohus.rift.generated.resources.window_satellite
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.generated.resources.window_sovereignty
import dev.nohus.rift.generated.resources.window_structures
import dev.nohus.rift.generated.resources.window_wallet
import dev.nohus.rift.generated.resources.window_warreport
import dev.nohus.rift.neocom.NeocomViewModel
import dev.nohus.rift.tray.TrayMenuItem.Separator
import dev.nohus.rift.tray.TrayMenuItem.TrayMenuTextItem
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun ApplicationScope.RiftTray(
    isVisible: Boolean,
) {
    if (isVisible) {
        val operatingSystem = remember { koin.get<OperatingSystem>() }

        val viewModel: NeocomViewModel = viewModel()
        val state by viewModel.state.collectAsState()

        val items = getTrayMenuItems(
            isJabberEnabled = state.isJabberEnabled,
            onButtonClick = viewModel::onButtonClick,
            onQuitClick = viewModel::onQuitClick,
        )

        val icon = if (isMenuBarInDarkMode()) Res.drawable.tray_tray_128 else Res.drawable.tray_tray_dark_128

        Tray(
            icon = icon,
            iconRenderProperties = getIconRenderProperties(operatingSystem),
            tooltip = "RIFT",
            primaryAction = { viewModel.onButtonClick(RiftWindow.Neocom) },
            menuContent = {
                for (item in items) {
                    when (item) {
                        Separator -> this.Divider()
                        is TrayMenuTextItem -> {
                            if (item.drawable != null) {
                                Item(
                                    label = item.text,
                                    icon = item.drawable,
                                    onClick = { item.action() },
                                )
                            } else {
                                Item(
                                    label = item.text,
                                    onClick = { item.action() },
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}

private fun getIconRenderProperties(operatingSystem: OperatingSystem): IconRenderProperties {
    val iconImageSize = 128

    val (targetWidth, targetHeight) = when (operatingSystem) {
        OperatingSystem.Windows -> 32 to 32
        OperatingSystem.MacOs -> 44 to 44
        OperatingSystem.Linux -> iconImageSize to iconImageSize
    }

    return IconRenderProperties(
        sceneWidth = iconImageSize,
        sceneHeight = iconImageSize,
        sceneDensity = Density(2f),
        targetWidth = targetWidth,
        targetHeight = targetHeight,
    )
}

sealed interface TrayMenuItem {
    data class TrayMenuTextItem(
        val text: String,
        val drawable: DrawableResource?,
        val action: () -> Unit,
    ) : TrayMenuItem

    data object Separator : TrayMenuItem
}

private fun getTrayMenuItems(
    isJabberEnabled: Boolean,
    onButtonClick: (RiftWindow) -> Unit,
    onQuitClick: () -> Unit,
): List<TrayMenuItem> {
    return buildList {
        add(TrayMenuTextItem("RIFT", Res.drawable.window_rift_64) { onButtonClick(RiftWindow.Neocom) })
        add(Separator)
        add(TrayMenuTextItem("Alerts", Res.drawable.window_loudspeaker_icon) { onButtonClick(RiftWindow.Alerts) })
        add(TrayMenuTextItem("Map", Res.drawable.window_map) { onButtonClick(RiftWindow.Map) })
        add(TrayMenuTextItem("Intel Feed", Res.drawable.window_satellite) { onButtonClick(RiftWindow.IntelFeed) })
        add(TrayMenuTextItem("Intel Reports", Res.drawable.window_warreport) { onButtonClick(RiftWindow.IntelReports) })
        add(TrayMenuTextItem("Characters", Res.drawable.window_characters) { onButtonClick(RiftWindow.Characters) })
        add(TrayMenuTextItem("Assets", Res.drawable.window_assets) { onButtonClick(RiftWindow.Assets) })
        add(TrayMenuTextItem("Wallets", Res.drawable.window_wallet) { onButtonClick(RiftWindow.Wallet) })
        add(TrayMenuTextItem("Planetary Industry", Res.drawable.window_planets) { onButtonClick(RiftWindow.PlanetaryIndustry) })
        add(TrayMenuTextItem("Opportunities", Res.drawable.window_opportunities) { onButtonClick(RiftWindow.Opportunities) })
        add(TrayMenuTextItem("Contacts", Res.drawable.window_contacts) { onButtonClick(RiftWindow.Contacts) })
        add(TrayMenuTextItem("Structures", Res.drawable.window_structures) { onButtonClick(RiftWindow.Structures) })
        add(TrayMenuTextItem("Chat", Res.drawable.window_bleedchannel) { onButtonClick(RiftWindow.Chat) })
        if (isJabberEnabled) {
            add(TrayMenuTextItem("Pings", Res.drawable.window_sovereignty) { onButtonClick(RiftWindow.Pings) })
            add(TrayMenuTextItem("Jabber", Res.drawable.window_chatchannels) { onButtonClick(RiftWindow.Jabber) })
        }
        add(TrayMenuTextItem("Jukebox", Res.drawable.window_jukebox) { onButtonClick(RiftWindow.Jukebox) })
        add(TrayMenuTextItem("Settings", Res.drawable.window_settings) { onButtonClick(RiftWindow.Settings) })
        add(TrayMenuTextItem("About", Res.drawable.window_evemailtag) { onButtonClick(RiftWindow.About) })
        add(Separator)
        add(TrayMenuTextItem("Quit", Res.drawable.window_quitgame) { onQuitClick() })
    }
}
