package dev.nohus.rift.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.FlagIcon
import dev.nohus.rift.compose.LinkText
import dev.nohus.rift.compose.MulticolorIconType
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftAutocompleteTextField
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftFileChooserButton
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftMulticolorIcon
import dev.nohus.rift.compose.RiftRadioButtonWithLabel
import dev.nohus.rift.compose.RiftSliderWithLabel
import dev.nohus.rift.compose.RiftSolarSystemChip
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTable
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.SectionTitle
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.TableCell
import dev.nohus.rift.compose.TableRow
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.configurationpack.ConfigurationPackRepository
import dev.nohus.rift.configurationpack.displayName
import dev.nohus.rift.di.koin
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitStandings
import dev.nohus.rift.generated.resources.*
import dev.nohus.rift.i18n.getStringSync
import dev.nohus.rift.notifications.NotificationEditWindow
import dev.nohus.rift.repositories.SolarSystemChipState
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.settings.SettingsViewModel.JumpBridgeCopyState
import dev.nohus.rift.settings.SettingsViewModel.JumpBridgeSearchState
import dev.nohus.rift.settings.SettingsViewModel.SettingsTab
import dev.nohus.rift.settings.SettingsViewModel.SovereigntyUpgradesCopyState
import dev.nohus.rift.settings.SettingsViewModel.UiState
import dev.nohus.rift.settings.persistence.CharacterPortraitsParallaxStrength
import dev.nohus.rift.settings.persistence.CharacterPortraitsStandingsTargets
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.standings.Standing
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.OperatingSystem.MacOs
import dev.nohus.rift.utils.formatDate
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.roundSecurity
import dev.nohus.rift.utils.toURIOrNull
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import javax.swing.JFileChooser
import kotlin.io.path.absolutePathString

@Composable
fun SettingsWindow(
    inputModel: SettingsInputModel,
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel(inputModel)
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = stringResource(Res.string.settings_window_title),
        icon = Res.drawable.window_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
        titleBarContent = { height ->
            ToolbarRow(
                selectedTab = state.selectedTab,
                fixedHeight = height,
                onTabSelected = viewModel::onTabSelected,
            )
        },
        withContentPadding = false,
        isResizable = false,
    ) {
        SettingsWindowContent(
            inputModel = inputModel,
            state = state,
            viewModel = viewModel,
        )

        state.dialogMessage?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }

        if (state.isJumpBridgeSearchDialogShown) {
            RiftDialog(
                title = stringResource(Res.string.settings_window_jump_bridge_search),
                icon = Res.drawable.window_warning,
                parentState = windowState,
                state = rememberWindowState(width = 350.dp, height = Dp.Unspecified),
                onCloseClick = viewModel::onJumpBridgeDialogDismissed,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    Text(
                        text = stringResource(Res.string.settings_window_jump_bridge_search_dialog),
                        style = RiftTheme.typography.bodyPrimary,
                    )
                    Text(
                        text = stringResource(Res.string.settings_window_jump_bridge_search_dialog_warn),
                        textAlign = TextAlign.Center,
                        style = RiftTheme.typography.headerPrimary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        RiftButton(
                            text = stringResource(Res.string.cancel),
                            cornerCut = ButtonCornerCut.BottomLeft,
                            type = ButtonType.Secondary,
                            onClick = viewModel::onJumpBridgeDialogDismissed,
                            modifier = Modifier.weight(1f),
                        )
                        RiftButton(
                            text = stringResource(Res.string.confirm),
                            type = ButtonType.Secondary,
                            onClick = viewModel::onJumpBridgeSearchDialogConfirmClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (state.isEditNotificationWindowOpen) {
        NotificationEditWindow(
            position = state.notificationEditPlacement,
            onCloseRequest = viewModel::onEditNotificationDone,
        )
    }
}

@Composable
private fun SettingsWindowContent(
    inputModel: SettingsInputModel,
    state: UiState,
    viewModel: SettingsViewModel,
) {
    Column {
        val offset = LocalDensity.current.run { 1.dp.toPx() }
        Box(
            modifier = Modifier
                .graphicsLayer(translationY = -offset)
                .fillMaxWidth()
                .height(1.dp)
                .background(RiftTheme.colors.borderGreyLight),
        )

        Layout(
            content = {
                // General Settings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .padding(Spacing.medium)
                        .height(IntrinsicSize.Max),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            UserInterfaceSection(state, viewModel)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel, SettingsInputModel.EveInstallation) {
                            EveInstallationSection(state, viewModel)
                        }
                        SectionContainer(inputModel) {
                            CharacterPortraitsSection(state, viewModel)
                        }
                    }
                }

                // Intel & Alerts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .padding(Spacing.medium)
                        .height(IntrinsicSize.Max),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel, SettingsInputModel.IntelChannels) {
                            IntelChannelsSection(state, viewModel)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            IntelTimeoutSection(state, viewModel)
                        }
                        SectionContainer(inputModel) {
                            AlertsSection(state, viewModel)
                        }
                        SectionContainer(inputModel) {
                            KillmailMonitoringSection(state, viewModel)
                        }
                    }
                }

                // Map & Autopilot
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .padding(Spacing.medium)
                        .height(IntrinsicSize.Max),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            MapUserInterfaceSection(state, viewModel)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            MapAutopilotSection(state, viewModel)
                        }
                        SectionContainer(inputModel) {
                            MapIntelPopupsSection(state, viewModel)
                        }
                    }
                }

                // Sovereignty
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .padding(Spacing.medium)
                        .height(IntrinsicSize.Max),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            JumpBridgeNetworkSection(state, viewModel)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            SovereigntyUpgradesSection(state, viewModel)
                        }
                    }
                }

                // Misc
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier
                        .padding(Spacing.medium)
                        .height(IntrinsicSize.Max),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            OtherSettingsSection(state, viewModel)
                        }
                        SectionContainer(inputModel) {
                            StorageSection(state, viewModel)
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.weight(1f),
                    ) {
                        SectionContainer(inputModel) {
                            ClipboardSection(state, viewModel)
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            val placeables = measurables.map { measurable ->
                measurable.measure(constraints)
            }
            val height = placeables.maxOf { it.height }
            layout(constraints.maxWidth, height) {
                placeables[state.selectedTab.id].place(0, 0)
            }
        }
    }
}

@Composable
private fun ToolbarRow(
    selectedTab: SettingsTab,
    fixedHeight: Dp,
    onTabSelected: (SettingsTab) -> Unit,
) {
    val tabs = remember {
        listOf(
            Tab(id = SettingsTab.General.id, title = getStringSync(Res.string.settings_window_bar_general_settings), isCloseable = false, payload = SettingsTab.General),
            Tab(id = SettingsTab.Intel.id, title = getStringSync(Res.string.settings_window_bar_intel), isCloseable = false, payload = SettingsTab.Intel),
            Tab(id = SettingsTab.Map.id, title = getStringSync(Res.string.settings_window_bar_map), isCloseable = false, payload = SettingsTab.Map),
            Tab(id = SettingsTab.Sovereignty.id, title = getStringSync(Res.string.settings_window_bar_sovereignty), isCloseable = false, payload = SettingsTab.Sovereignty),
            Tab(id = SettingsTab.Misc.id, title = "Misc", isCloseable = false, payload = SettingsTab.Misc),
        )
    }
    RiftTabBar(
        tabs = tabs,
        selectedTab = selectedTab.id,
        onTabSelected = { tab ->
            onTabSelected(tabs.first { it.id == tab }.payload as SettingsTab)
        },
        onTabClosed = {},
        withUnderline = false,
        withWideTabs = true,
        fixedHeight = fixedHeight,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionContainer(
    inputModel: SettingsInputModel,
    enabledInputModel: SettingsInputModel? = null,
    content: @Composable () -> Unit,
) {
    val isEnabled = inputModel == SettingsInputModel.Normal || inputModel == enabledInputModel
    Box(
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .modifyIf(!isEnabled) {
                alpha(0.3f)
            },
    ) {
        Column {
            content()
        }
        if (!isEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onClick {},
            ) {}
        }
    }
}

@Composable
private fun UserInterfaceSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_user_interface_section_title), Modifier.padding(bottom = Spacing.medium))
    RiftCheckboxWithLabel(
        label = stringResource(Res.string.settings_window_remeber_open_window),
        tooltip = stringResource(Res.string.settings_window_remeber_open_window_tooltip),
        isChecked = state.isRememberOpenWindows,
        onCheckedChange = viewModel::onRememberOpenWindowsChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = stringResource(Res.string.settings_window_remeber_window_placement),
        tooltip = stringResource(Res.string.settings_window_remeber_window_placement_tooltip),
        isChecked = state.isRememberWindowPlacement,
        onCheckedChange = viewModel::onRememberWindowPlacementChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = stringResource(Res.string.settings_window_display_eve_time),
        tooltip = stringResource(Res.string.settings_window_display_eve_time_tooltip),
        isChecked = state.isDisplayEveTime,
        onCheckedChange = viewModel::onIsDisplayEveTimeChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = stringResource(Res.string.settings_window_dark_tray),
        tooltip = stringResource(Res.string.settings_window_dark_tray_tooltip),
        isChecked = state.isUsingDarkTrayIcon,
        onCheckedChange = viewModel::onIsUsingDarkTrayIconChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = stringResource(Res.string.settings_window_isk_cents),
        tooltip = stringResource(Res.string.settings_window_isk_cents_tooltip),
        isChecked = state.isShowIskCents,
        onCheckedChange = viewModel::onIsShowIskCentsChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    if (koin.get<OperatingSystem>() != MacOs) {
        RiftCheckboxWithLabel(
            label = stringResource(Res.string.settings_window_smart_always_above),
            tooltip = stringResource(Res.string.settings_window_smart_always_above_tooltip),
            isChecked = state.isSmartAlwaysAbove,
            onCheckedChange = viewModel::onIsSmartAlwaysAboveChanged,
            modifier = Modifier.padding(bottom = Spacing.small),
        )
    }
    RiftCheckboxWithLabel(
        label = stringResource(Res.string.settings_window_show_distance_on_systems),
        tooltip = stringResource(Res.string.settings_window_show_distance_on_systems_tooltip),
        isChecked = state.isShowingSystemDistance,
        onCheckedChange = viewModel::onIsShowingSystemDistanceChange,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = stringResource(Res.string.settings_window_use_jump_bridge),
        tooltip = stringResource(Res.string.settings_window_show_distance_on_systems_tooltip),
        isChecked = state.isUsingJumpBridgesForDistance,
        onCheckedChange = viewModel::onIsUsingJumpBridgesForDistance,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = stringResource(Res.string.settings_window_transparent_windows),
        tooltip = stringResource(Res.string.settings_window_transparent_windows_tooltip),
        isChecked = state.isWindowTransparencyEnabled,
        onCheckedChange = viewModel::onIsWindowTransparencyChanged,
    )
    Spacer(Modifier.height(Spacing.small))
    RiftDropdownWithLabel(
        label = stringResource(Res.string.settings_window_window_transparency),
        items = listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        selectedItem = state.windowTransparencyModifier,
        onItemSelected = viewModel::onWindowTransparencyModifierChanged,
        getItemName = {
            when (it) {
                0f -> getStringSync(Res.string.settings_window_maximal)
                0.25f -> getStringSync(Res.string.settings_window_high)
                0.5f -> getStringSync(Res.string.settings_window_medium)
                0.75f -> getStringSync(Res.string.settings_window_low)
                1f -> getStringSync(Res.string.settings_window_minimal)
                else -> getStringSync(Res.string.settings_window_custom)
            }
        },
    )
    RiftDropdownWithLabel(
        label = stringResource(Res.string.settings_window_ui_scale),
        items = listOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f),
        selectedItem = state.uiScale,
        onItemSelected = viewModel::onUiScaleChanged,
        getItemName = { String.format("%d%%", (it * 100).toInt()) },
    )
    RiftDropdownWithLabel(
        label = stringResource(Res.string.language),
        items = listOf(Locale.ENGLISH, Locale.CHINESE),
        selectedItem = state.language,
        onItemSelected = viewModel::onLanguageChanged,
        getItemName = { it.getDisplayLanguage(it) }
    )
}

@Composable
private fun AlertsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_alerts_section_title), Modifier.padding(bottom = Spacing.medium))
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.settings_window_alerts_choose_notification_position),
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        RiftButton(
            text = stringResource(Res.string.settings_window_alerts_edit_notification_position),
            onClick = viewModel::onEditNotificationClick,
        )
    }
    RiftSliderWithLabel(
        label = stringResource(Res.string.settings_window_alerts_volume),
        width = 100.dp,
        range = 0..100,
        currentValue = state.soundsVolume,
        onValueChange = viewModel::onSoundsVolumeChange,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.settings_window_alerts_mobile_push_notifaications),
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        RiftButton(
            text = stringResource(Res.string.settings_window_alerts_mobile_push_notifaications_configure),
            onClick = viewModel::onConfigurePushoverClick,
        )
    }
}

@Composable
private fun KillmailMonitoringSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_killmail_monitor_section_title), Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = stringResource(Res.string.settings_window_killmail_monitor_description),
            style = RiftTheme.typography.bodySecondary,
        )
        RiftCheckboxWithLabel(
            label = stringResource(Res.string.settings_window_killmail_monitor_label),
            tooltip = stringResource(Res.string.settings_window_killmail_monitor_tooltip),
            isChecked = state.isZkillboardMonitoringEnabled,
            onCheckedChange = viewModel::onIsZkillboardMonitoringChanged,
            modifier = Modifier.padding(bottom = Spacing.small),
        )
    }
}

@Composable
private fun OtherSettingsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_other_settings_section_title), Modifier.padding(bottom = Spacing.medium))
    RiftDropdownWithLabel(
        label = stringResource(Res.string.settings_window_configuaration_pack),
        items = listOf(null) + ConfigurationPack.entries,
        selectedItem = state.configurationPack,
        onItemSelected = viewModel::onConfigurationPackChange,
        getItemName = { it?.displayName ?: getStringSync(Res.string.default) },
        tooltip = stringResource(Res.string.settings_window_configuaration_pack_tooltip),
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = stringResource(Res.string.settings_window_show_setup_wizard_on_nextstart),
        tooltip = stringResource(Res.string.settings_window_show_setup_wizard_on_nextstart_tooltip),
        isChecked = state.isShowSetupWizardOnNextStartEnabled,
        onCheckedChange = viewModel::onShowSetupWizardOnNextStartChanged,
    )
}

@Composable
private fun StorageSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_storage_section_title), Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(Res.string.settings_window_rift_data_directory),
                    style = RiftTheme.typography.bodyPrimary,
                )
                Text(
                    text = state.storageStats?.dataDirectory?.toString() ?: "…",
                    style = RiftTheme.typography.detailSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                )
            }
            RiftButton(
                text = stringResource(Res.string.settings_window_open_data_directory),
                type = ButtonType.Primary,
                onClick = viewModel::onOpenAppData,
            )
        }
        UsedSpace(stringResource(Res.string.settings_window_all_data), state.storageStats?.dataSize)

        Divider(color = RiftTheme.colors.divider, modifier = Modifier.padding(vertical = Spacing.small))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(Res.string.settings_window_rift_cache_directory),
                    style = RiftTheme.typography.bodyPrimary,
                )
                Text(
                    text = state.storageStats?.cacheDirectory?.toString() ?: "…",
                    style = RiftTheme.typography.detailSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                )
            }
            RiftButton(
                text = stringResource(Res.string.settings_window_open_cache_directory),
                type = ButtonType.Primary,
                onClick = viewModel::onOpenAppCache,
            )
        }
        UsedSpace(stringResource(Res.string.settings_window_esi_cache), state.storageStats?.esiCacheSize)
        UsedSpace(stringResource(Res.string.settings_window_km_cache), state.storageStats?.zkillCacheSize)
        UsedSpace(stringResource(Res.string.settings_window_http_cache), state.storageStats?.httpCacheSize)
        UsedSpace(stringResource(Res.string.settings_window_portraits_cache), state.storageStats?.portraitsSize, state.storageStats?.portraitsCount?.let { "$it characters," })
        UsedSpace(stringResource(Res.string.settings_window_other_cache), state.storageStats?.otherCacheSize)
    }
}

@Composable
private fun UsedSpace(text: String, bytes: Long?, secondaryText: String? = null) {
    Row {
        Text(
            text = text,
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        if (secondaryText != null) {
            Text(
                text = secondaryText,
                style = RiftTheme.typography.bodyPrimary,
                modifier = Modifier.padding(end = Spacing.small),
            )
        }
        if (bytes != null) {
            Text(
                text = buildAnnotatedString {
                    append(formatBytes(bytes))
                    withColor(RiftTheme.colors.textSecondary) {
                        append(getStringSync(Res.string.settings_window_cache_used))
                    }
                },
                style = RiftTheme.typography.bodyPrimary,
            )
        } else {
            Text(
                text = stringResource(Res.string.settings_window_calculating_cache),
                style = RiftTheme.typography.bodySecondary,
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024f)
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024f * 1024))
        else -> String.format("%.2f GB", bytes / (1024f * 1024 * 1024))
    }
}

@Composable
private fun ClipboardSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_clipboard_section_title), Modifier.padding(bottom = Spacing.medium))
    Text(
        text = stringResource(Res.string.settings_window_clipboard_section_description),
        style = RiftTheme.typography.bodySecondary,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.settings_window_clipboard_troubleshoot))
        RiftButton(
            text = stringResource(Res.string.settings_window_clipboard_troubleshoot_button),
            type = ButtonType.Primary,
            onClick = viewModel::onClipboardTesterClick,
        )
    }
}

@Composable
private fun EveInstallationSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_eve_instanllation_section_title))
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.settings_window_eve_log_directory),
            style = RiftTheme.typography.bodyPrimary,
        )
        RequirementIcon(
            isFulfilled = state.isLogsDirectoryValid,
            fulfilledTooltip = stringResource(Res.string.settings_window_eve_log_directory_valid),
            notFulfilledTooltip = if (state.logsDirectory.isBlank()) stringResource(Res.string.settings_window_eve_log_directory_blank) else stringResource(Res.string.settings_window_eve_log_directory_invalid),
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        var text by remember(state.logsDirectory) { mutableStateOf(state.logsDirectory) }
        RiftTextField(
            text = text,
            onTextChanged = {
                text = it
                viewModel.onLogsDirectoryChanged(it)
            },
            modifier = Modifier.weight(1f),
        )
        RiftFileChooserButton(
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY,
            typesDescription = stringResource(Res.string.settings_window_eve_chat_log_directory),
            currentPath = text,
            type = ButtonType.Secondary,
            cornerCut = ButtonCornerCut.None,
            onFileChosen = {
                text = it.absolutePathString()
                viewModel.onLogsDirectoryChanged(it.absolutePathString())
            },
        )
        RiftButton(
            text = stringResource(Res.string.settings_window_eve_directory_detect),
            type = if (state.isLogsDirectoryValid) ButtonType.Secondary else ButtonType.Primary,
            onClick = viewModel::onDetectLogsDirectoryClick,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.settings_window_eve_character_settings_directory),
            style = RiftTheme.typography.bodyPrimary,
        )
        RequirementIcon(
            isFulfilled = state.isSettingsDirectoryValid,
            fulfilledTooltip = stringResource(Res.string.settings_window_eve_character_settings_directory_valid),
            notFulfilledTooltip = if (state.settingsDirectory.isBlank()) stringResource(Res.string.settings_window_eve_character_settings_directory_blank) else stringResource(Res.string.settings_window_eve_character_settings_directory_invalid),
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        var text by remember(state.settingsDirectory) { mutableStateOf(state.settingsDirectory) }
        RiftTextField(
            text = text,
            onTextChanged = {
                text = it
                viewModel.onSettingsDirectoryChanged(it)
            },
            modifier = Modifier.weight(1f),
        )
        RiftFileChooserButton(
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY,
            typesDescription = stringResource(Res.string.settings_window_eve_game_log_directory),
            currentPath = text,
            type = ButtonType.Secondary,
            cornerCut = ButtonCornerCut.None,
            onFileChosen = {
                text = it.absolutePathString()
                viewModel.onSettingsDirectoryChanged(it.absolutePathString())
            },
        )
        RiftButton(
            text = stringResource(Res.string.settings_window_eve_directory_detect),
            type = if (state.isSettingsDirectoryValid) ButtonType.Secondary else ButtonType.Primary,
            onClick = viewModel::onDetectSettingsDirectoryClick,
        )
    }
}

@Composable
private fun CharacterPortraitsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_character_portrait_section_title))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(top = Spacing.small),
        ) {
            listOf(91217127, 2123140346, 2119893075, 2118421377).forEach {
                DynamicCharacterPortraitParallax(
                    characterId = it,
                    size = 48.dp,
                    enterTimestamp = null,
                    pointerInteractionStateHolder = null,
                )
            }
        }
        RiftDropdownWithLabel(
            label = stringResource(Res.string.settings_window_character_parallax_effect_label),
            items = CharacterPortraitsParallaxStrength.entries,
            selectedItem = state.characterPortraits.parallaxStrength,
            onItemSelected = { viewModel.onCharacterPortraitsParallaxStrengthChanged(it) },
            getItemName = {
                when (it) {
                    CharacterPortraitsParallaxStrength.None -> getStringSync(Res.string.settings_window_character_parallax_effect_none)
                    CharacterPortraitsParallaxStrength.Reduced -> getStringSync(Res.string.settings_window_character_parallax_effect_reduced)
                    CharacterPortraitsParallaxStrength.Normal -> getStringSync(Res.string.settings_window_character_parallax_effect_normal)
                }
            },
            tooltip = getStringSync(Res.string.settings_window_character_parallax_effect_toltip)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Standing.entries.forEach { standing ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    FlagIcon(standing)
                    DynamicCharacterPortraitStandings(
                        characterId = 324677773,
                        size = 32.dp,
                        standingLevel = standing,
                        isAnimated = true,
                    )
                }
            }
        }
        RiftDropdownWithLabel(
            label = stringResource(Res.string.settings_window_character_standing_background_label),
            items = CharacterPortraitsStandingsTargets.entries,
            selectedItem = state.characterPortraits.standingsTargets,
            onItemSelected = { viewModel.onCharacterPortraitsStandingsTargetsChanged(it) },
            getItemName = {
                when (it) {
                    CharacterPortraitsStandingsTargets.All -> getStringSync(Res.string.settings_window_character_standing_background_all)
                    CharacterPortraitsStandingsTargets.OnlyFriendly -> getStringSync(Res.string.settings_window_character_standing_background_only_friendly)
                    CharacterPortraitsStandingsTargets.OnlyHostile -> getStringSync(Res.string.settings_window_character_standing_background_only_hostile)
                    CharacterPortraitsStandingsTargets.OnlyNonNeutral -> getStringSync(Res.string.settings_window_character_standing_background_only_non_neutral)
                    CharacterPortraitsStandingsTargets.None -> getStringSync(Res.string.settings_window_character_standing_background_only_none)
                }
            },
            tooltip = getStringSync(Res.string.settings_window_character_standing_background_tooltip),
        )
        RiftSliderWithLabel(
            label = getStringSync(Res.string.settings_window_character_standing_background_strength),
            width = 100.dp,
            range = 30..100,
            currentValue = (state.characterPortraits.standingsEffectStrength * 100).toInt().coerceIn(0..100),
            onValueChange = { viewModel.onCharacterPortraitsStandingsEffectStrengthChanged(it / 100f) },
            getValueName = { "$it%" },
        )
    }
}

@Composable
private fun IntelChannelsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_intel_channels))
    Text(
        text = stringResource(Res.string.settings_window_intel_channels_tooltip),
        style = RiftTheme.typography.bodyPrimary,
        modifier = Modifier.padding(vertical = Spacing.medium),
    )
    ScrollbarColumn(
        modifier = Modifier
            .height(300.dp)
            .border(1.dp, RiftTheme.colors.borderGrey),
        scrollbarModifier = Modifier.padding(vertical = Spacing.small),
    ) {
        for (channel in state.intelChannels) {
            key(channel) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .hoverBackground()
                        .padding(Spacing.small),
                ) {
                    val text = buildAnnotatedString {
                        append(channel.name)
                        withStyle(SpanStyle(color = RiftTheme.colors.textSecondary)) {
                            append(" – ${channel.region ?: "All regions"}")
                        }
                    }
                    Text(
                        text = text,
                        style = RiftTheme.typography.bodyPrimary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    RiftImageButton(
                        resource = Res.drawable.deleteicon,
                        size = 20.dp,
                        onClick = { viewModel.onIntelChannelDelete(channel) },
                    )
                }
            }
        }
        if (state.intelChannels.isEmpty()) {
            Text(
                text = stringResource(Res.string.settings_window_no_intel_channels_configured),
                style = RiftTheme.typography.headerPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.large)
                    .padding(horizontal = Spacing.large),
            )
            if (state.suggestedIntelChannels != null) {
                Text(
                    text = state.suggestedIntelChannels.promptTitleText,
                    style = RiftTheme.typography.bodyPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.medium)
                        .padding(horizontal = Spacing.large),
                )
                RiftButton(
                    text = state.suggestedIntelChannels.promptButtonText,
                    type = ButtonType.Primary,
                    cornerCut = ButtonCornerCut.Both,
                    onClick = viewModel::onSuggestedIntelChannelsClick,
                    modifier = Modifier
                        .padding(top = Spacing.medium)
                        .align(Alignment.CenterHorizontally),
                )
            }
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(top = Spacing.medium),
    ) {
        var addChannelText by remember { mutableStateOf("") }
        RiftAutocompleteTextField(
            text = addChannelText,
            suggestions = state.autocompleteIntelChannels.filter { it.lowercase().startsWith(addChannelText.lowercase()) }.take(5),
            placeholder = stringResource(Res.string.settings_window_channel_name),
            onTextChanged = {
                addChannelText = it
            },
            modifier = Modifier.weight(1f),
        )
        val regionPlaceholder = stringResource(Res.string.settings_window_choose_region)
        var selectedRegion by remember { mutableStateOf<String?>(regionPlaceholder) }
        RiftDropdown(
            items = listOf(null) + state.regions,
            selectedItem = selectedRegion,
            onItemSelected = { selectedRegion = it },
            getItemName = { it ?: "All regions" },
            maxItems = 5,
        )

        val isNameSelected = addChannelText.isNotEmpty()
        val isRegionSelected = selectedRegion != regionPlaceholder
        RiftTooltipArea(
            text = if (!isNameSelected) {
                stringResource(Res.string.settings_window_enter_channel_name)
            } else if (!isRegionSelected) {
                stringResource(Res.string.settings_window_choose_a_region)
            } else {
                null
            },
        ) {
            RiftButton(
                text = stringResource(Res.string.settings_window_add_channel),
                isEnabled = isNameSelected && isRegionSelected,
                onClick = {
                    if (addChannelText.isNotEmpty() && selectedRegion != regionPlaceholder) {
                        viewModel.onIntelChannelAdded(addChannelText, selectedRegion)
                        addChannelText = ""
                        selectedRegion = regionPlaceholder
                    }
                },
            )
        }
    }
}

@Composable
private fun IntelTimeoutSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_intel_timeout), Modifier.padding(bottom = Spacing.medium))
    val expiryItems = mapOf(
        stringResource(Res.string.time_1min) to 60,
        stringResource(Res.string.time_2min) to 60 * 2,
        stringResource(Res.string.time_5min) to 60 * 5,
        stringResource(Res.string.time_10min) to 60 * 10,
        stringResource(Res.string.time_15min) to 60 * 15,
        stringResource(Res.string.time_30min) to 60 * 30,
        stringResource(Res.string.time_1h) to 60 * 60,
        stringResource(Res.string.settings_window_intel_timeout_dont_expire) to Int.MAX_VALUE,
    )
    RiftDropdownWithLabel(
        label = stringResource(Res.string.settings_window_expire_intel_after),
        items = expiryItems.values.toList(),
        selectedItem = state.intelExpireSeconds,
        onItemSelected = viewModel::onIntelExpireSecondsChange,
        getItemName = { item -> expiryItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
        tooltip = stringResource(Res.string.settings_window_expire_intel_after_tooltip),
    )
}

@Composable
private fun MapUserInterfaceSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_map_ui_section_title), Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        RiftCheckboxWithLabel(
            label = stringResource(Res.string.compact_mode),
            isChecked = state.intelMap.isUsingCompactMode,
            onCheckedChange = viewModel::onIsUsingCompactModeChange,
        )
        RiftCheckboxWithLabel(
            label = stringResource(Res.string.settings_window_map_follow_character),
            tooltip = stringResource(Res.string.settings_window_map_follow_character_tooltip),
            isChecked = state.intelMap.isFollowingCharacterWithinLayouts,
            onCheckedChange = { viewModel.onIsFollowingCharacterWithinLayoutsChange(it) },
        )
        RiftCheckboxWithLabel(
            label = stringResource(Res.string.settings_window_map_switch_follow_character),
            tooltip = stringResource(Res.string.settings_window_map_switch_follow_character_tooltip),
            isChecked = state.intelMap.isFollowingCharacterAcrossLayouts,
            onCheckedChange = { viewModel.onIsFollowingCharacterAcrossLayoutsChange(it) },
        )
        RiftCheckboxWithLabel(
            label = stringResource(Res.string.settings_window_invert_scroll_wheel_zoom),
            tooltip = stringResource(Res.string.settings_window_invert_scroll_wheel_zoom_tooltip),
            isChecked = state.intelMap.isInvertZoom,
            onCheckedChange = { viewModel.onIsScrollZoomInvertedChange(it) },
        )
        RiftCheckboxWithLabel(
            label = stringResource(Res.string.settings_window_show_system_labels),
            tooltip = stringResource(Res.string.settings_window_show_system_labels_tooltip),
            isChecked = state.intelMap.isAlwaysShowingSystems,
            onCheckedChange = { viewModel.onIsAlwaysShowingSystemsChange(it) },
        )
        RiftCheckboxWithLabel(
            label = stringResource(Res.string.settings_window_prefer_show_system_on_region_map),
            tooltip = stringResource(Res.string.settings_window_prefer_show_system_on_region_map_tooltip),
            isChecked = state.intelMap.isPreferringRegionMaps,
            onCheckedChange = { viewModel.onIsPreferringRegionMapsChange(it) },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(end = Spacing.medium).fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.settings_window_view_edit_map_markers))
            RiftButton(
                text = stringResource(Res.string.settings_window_map_markers),
                type = ButtonType.Primary,
                onClick = viewModel::onMapNotesClick,
            )
        }
        Text(
            text = buildAnnotatedString {
                withColor(RiftTheme.colors.textPrimary) {
                    append(getStringSync(Res.string.settings_window_tip))
                }
                append(getStringSync(Res.string.settings_window_press_space_tip))
            },
            style = RiftTheme.typography.bodySecondary,
        )
    }
}

@Composable
private fun MapAutopilotSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_autopilot_section_title), Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = stringResource(Res.string.settings_window_autopilot_section_tooltip),
            style = RiftTheme.typography.bodySecondary,
        )
        RiftRadioButtonWithLabel(
            label = stringResource(Res.string.settings_window_autopilot_rift_calculated),
            tooltip = stringResource(Res.string.settings_window_autopilot_rift_calculated_tooltip),
            isChecked = state.isUsingRiftAutopilotRoute,
            onChecked = { viewModel.onIsUsingRiftAutopilotRouteChange(true) },
        )
        RiftRadioButtonWithLabel(
            label = stringResource(Res.string.settings_window_autopilot_eve_calculated),
            tooltip = stringResource(Res.string.settings_window_autopilot_eve_calculated_tooltip),
            isChecked = !state.isUsingRiftAutopilotRoute,
            onChecked = { viewModel.onIsUsingRiftAutopilotRouteChange(false) },
        )
    }
}

@Composable
private fun MapIntelPopupsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_intel_popups_section_title), Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        val timeoutItems = mapOf(
            stringResource(Res.string.settings_window_dont_show) to 0,
            stringResource(Res.string.time_10s) to 10,
            stringResource(Res.string.time_30s) to 30,
            stringResource(Res.string.time_1min) to 60,
            stringResource(Res.string.time_2min) to 60 * 2,
            stringResource(Res.string.time_5min) to 60 * 5,
            stringResource(Res.string.time_15min) to 60 * 15,
            stringResource(Res.string.settings_window_no_limit) to Int.MAX_VALUE,
        )
        RiftDropdownWithLabel(
            label = stringResource(Res.string.settings_window_auto_show_popups),
            items = timeoutItems.values.toList(),
            selectedItem = state.intelMap.intelPopupTimeoutSeconds,
            onItemSelected = viewModel::onIntelPopupTimeoutSecondsChange,
            getItemName = { item -> timeoutItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
            tooltip = stringResource(Res.string.settings_window_auto_show_popups_tooltip),
        )
    }
}

@Composable
private fun JumpBridgeNetworkSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_jump_bridge_network_section_title), Modifier.padding())
    Column {
        val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
        ScrollbarLazyColumn(
            modifier = Modifier
                .height(250.dp)
                .border(1.dp, RiftTheme.colors.borderGrey),
            scrollbarModifier = Modifier.padding(vertical = Spacing.small),
            contentPadding = PaddingValues(vertical = Spacing.verySmall),
        ) {
            if (state.jumpBridgeNetwork.isNotEmpty()) {
                val connections = state.jumpBridgeNetwork.sortedBy { it.from.name }
                for (connection in connections) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .hoverBackground()
                                .padding(horizontal = Spacing.small, vertical = Spacing.verySmall),
                        ) {
                            RiftSolarSystemChip(
                                state = SolarSystemChipState(
                                    locationsText = null,
                                    jumpsText = null,
                                    name = connection.from.name,
                                    security = connection.from.security.roundSecurity(),
                                    region = solarSystemsRepository.getRegionBySystem(connection.from.name)?.name,
                                ),
                                hasBackground = false,
                            )
                            Text(
                                text = "→",
                                style = RiftTheme.typography.bodyPrimary,
                            )
                            RiftSolarSystemChip(
                                state = SolarSystemChipState(
                                    locationsText = null,
                                    jumpsText = null,
                                    name = connection.to.name,
                                    security = connection.to.security.roundSecurity(),
                                    region = solarSystemsRepository.getRegionBySystem(connection.to.name)?.name,
                                ),
                                hasBackground = false,
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = stringResource(Res.string.settings_window_no_jump_bridge),
                        style = RiftTheme.typography.headerPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.large)
                            .padding(horizontal = Spacing.large),
                    )
                    AnimatedContent(state.jumpBridgeCopyState) { copyState ->
                        when (copyState) {
                            JumpBridgeCopyState.NotCopied -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Spacing.medium),
                                ) {
                                    Text(stringResource(Res.string.settings_window_import_jump_bridges_tip1))
                                    when (state.jumpBridgesReference) {
                                        is ConfigurationPackRepository.JumpBridgesReference.Url -> {
                                            Text(stringResource(Res.string.settings_window_import_jump_bridges_tip2))
                                            LinkText(
                                                text = stringResource(Res.string.settings_window_import_jump_bridges_list),
                                                onClick = { state.jumpBridgesReference.url.toURIOrNull()?.openBrowser() },
                                            )
                                        }
                                        is ConfigurationPackRepository.JumpBridgesReference.Text -> {
                                            Text(
                                                text = "A list of jump bridges for ${state.jumpBridgesReference.packName} from ${formatDate(state.jumpBridgesReference.date)} is available",
                                                textAlign = TextAlign.Center,
                                            )
                                            LinkText(
                                                text = "Click to use it",
                                                onClick = { Clipboard.copy(state.jumpBridgesReference.text) },
                                            )
                                        }
                                        null -> {
                                            val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
                                            RiftTooltipArea(
                                                text = buildAnnotatedString {
                                                    appendLine(stringResource(Res.string.settings_window_import_jump_bridges_tip3))
                                                    appendLine()
                                                    withColor(RiftTheme.colors.textHighlighted) {
                                                        appendLine("Jita -> Perimeter")
                                                        appendLine("New Caldari -> Alikara")
                                                        append("Hirtamon -> Ikuchi")
                                                    }
                                                },
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                                    modifier = Modifier
                                                        .pointerInteraction(pointerInteractionStateHolder)
                                                        .padding(vertical = Spacing.small),
                                                ) {
                                                    Text(
                                                        text = stringResource(Res.string.settings_window_format_info),
                                                        style = RiftTheme.typography.bodySecondary,
                                                    )
                                                    RiftMulticolorIcon(
                                                        type = MulticolorIconType.Info,
                                                        parentPointerInteractionStateHolder = pointerInteractionStateHolder,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is JumpBridgeCopyState.Copied -> {
                                val tooltip = buildString {
                                    val connections = copyState.network.take(5).joinToString("\n") {
                                        "${it.from.name} → ${it.to.name}"
                                    }
                                    append(connections)
                                    if (copyState.network.size > 5) {
                                        appendLine()
                                        append(stringResource(Res.string.settings_window_add_more))
                                    }
                                }
                                RiftTooltipArea(
                                    text = tooltip,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = Spacing.medium),
                                    ) {
                                        Text(stringResource(Res.string.settings_window_copied_network))
                                        RiftButton(
                                            text = stringResource(Res.string.settings_window_copied_network_count, copyState.network.size),
                                            onClick = viewModel::onJumpBridgeImportClick,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedContent(state.jumpBridgeNetwork) { network ->
            if (network.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = Spacing.medium).fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.settings_window_jump_bridge_count, network.size))
                    Spacer(Modifier.weight(1f))
                    RiftButton(
                        text = stringResource(Res.string.copy),
                        type = ButtonType.Primary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = viewModel::onJumpBridgeCopyClick,
                        modifier = Modifier.padding(end = Spacing.medium),
                    )
                    RiftButton(
                        text = stringResource(Res.string.forget),
                        type = ButtonType.Negative,
                        onClick = viewModel::onJumpBridgeForgetClick,
                    )
                }
            }
        }
        AnimatedContent(state.jumpBridgeSearchState, contentKey = { it::class }) { searchState ->
            when (searchState) {
                JumpBridgeSearchState.NotSearched -> {
                    if (state.jumpBridgeNetwork.isEmpty()) {
                        Column(
                            modifier = Modifier.padding(top = Spacing.medium),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(Res.string.settings_window_auto_search))
                                RiftButton(
                                    text = stringResource(Res.string.search),
                                    onClick = viewModel::onJumpBridgeSearchClick,
                                )
                            }
                        }
                    }
                }
                is JumpBridgeSearchState.Searching -> {
                    Column(
                        modifier = Modifier.padding(top = Spacing.medium),
                    ) {
                        val progressPercentage = String.format("%.1f", searchState.progress * 100)
                        Text(stringResource(Res.string.settings_window_searching_jump_bridges, progressPercentage))
                        Text(
                            text = stringResource(Res.string.settings_window_searching_jump_bridges_count, searchState.connectionsCount),
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
                JumpBridgeSearchState.SearchFailed -> {
                    Column(
                        modifier = Modifier.padding(top = Spacing.medium),
                    ) {
                        Text(stringResource(Res.string.settings_window_unable_search))
                    }
                }
                is JumpBridgeSearchState.SearchDone -> {
                    Column(
                        modifier = Modifier.padding(top = Spacing.medium),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.settings_window_found_network_count, searchState.network.size))
                            RiftButton(
                                text = stringResource(Res.string.import),
                                onClick = viewModel::onJumpBridgeSearchImportClick,
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(state.jumpBridgeNetwork.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier.padding(top = Spacing.medium),
            ) {
                RiftCheckboxWithLabel(
                    label = stringResource(Res.string.settings_window_show_network_on_map),
                    tooltip = stringResource(Res.string.settings_window_show_network_on_map_tooltip),
                    isChecked = state.intelMap.isJumpBridgeNetworkShown,
                    onCheckedChange = viewModel::onIsJumpBridgeNetworkShownChange,
                )
                RiftSliderWithLabel(
                    label = stringResource(Res.string.settings_window_connetion_opacity),
                    width = 100.dp,
                    range = 10..100,
                    currentValue = state.intelMap.jumpBridgeNetworkOpacity,
                    onValueChange = viewModel::onJumpBridgeNetworkOpacityChange,
                    getValueName = { "$it%" },
                    tooltip = stringResource(Res.string.settings_window_connection_opacity_tooltip),
                )
            }
        }
    }
}

@Composable
private fun SovereigntyUpgradesSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle(stringResource(Res.string.settings_window_sovereignty_upgrade_section_title), Modifier.padding())
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
        ScrollbarLazyColumn(
            modifier = Modifier
                .height(250.dp)
                .border(1.dp, RiftTheme.colors.borderGrey),
            scrollbarModifier = Modifier.padding(vertical = Spacing.small),
            contentPadding = PaddingValues(vertical = Spacing.verySmall),
        ) {
            if (state.sovereigntyUpgrades.isNotEmpty()) {
                for ((system, upgrades) in state.sovereigntyUpgrades) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .hoverBackground()
                                .padding(horizontal = Spacing.small, vertical = Spacing.verySmall),
                        ) {
                            RiftSolarSystemChip(
                                state = SolarSystemChipState(
                                    locationsText = null,
                                    jumpsText = null,
                                    name = system.name,
                                    security = system.security,
                                    region = solarSystemsRepository.getRegionBySystem(system.name)?.name,
                                ),
                                hasBackground = false,
                            )
                            for (type in upgrades) {
                                RiftTooltipArea(
                                    text = type.name,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    AsyncTypeIcon(
                                        type = type,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = stringResource(Res.string.settings_window_no_sovereignty_upgrades_imported),
                        style = RiftTheme.typography.headerPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.large)
                            .padding(horizontal = Spacing.large),
                    )
                    AnimatedContent(state.sovereigntyUpgradesCopyState) { copyState ->
                        when (copyState) {
                            SovereigntyUpgradesCopyState.NotCopied -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Spacing.medium),
                                ) {
                                    Text(stringResource(Res.string.settings_window_import_sovereignty_upgrades_tip1))
                                    if (state.sovereigntyUpgradesUrl != null) {
                                        Text(
                                            text = stringResource(Res.string.settings_window_import_sovereignty_upgrades_tip2),
                                            textAlign = TextAlign.Center,
                                        )
                                        LinkText(
                                            text = stringResource(Res.string.settings_window_alliance_sovereignty_upgrades_list),
                                            onClick = { state.sovereigntyUpgradesUrl.toURIOrNull()?.openBrowser() },
                                        )
                                    } else {
                                        Text(stringResource(Res.string.settings_window_import_sovereignty_upgrades_tip3))
                                    }
                                }
                            }
                            is SovereigntyUpgradesCopyState.Copied -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Spacing.medium),
                                ) {
                                    Text(stringResource(Res.string.settings_window_copied_upgrades))
                                    RiftButton(
                                        text = stringResource(Res.string.settings_window_copied_upgrades_count, copyState.upgrades.size),
                                        onClick = viewModel::onSovereigntyUpgradesImportClick,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedContent(state.sovereigntyUpgrades) { upgrades ->
            if (upgrades.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = Spacing.medium).fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.settings_window_upgrade_systems_count, upgrades.size, upgrades.size))
                    Spacer(Modifier.weight(1f))
                    RiftButton(
                        text = stringResource(Res.string.copy),
                        type = ButtonType.Primary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = viewModel::onSovereigntyUpgradesCopyClick,
                        modifier = Modifier.padding(end = Spacing.medium),
                    )
                    RiftButton(
                        text = stringResource(Res.string.forget),
                        type = ButtonType.Negative,
                        onClick = viewModel::onSovereigntyUpgradesForgetClick,
                    )
                }
            }
        }

        RiftCheckboxWithLabel(
            label = stringResource(Res.string.settings_window_import_upgrades_from_hacked_sov_hubs),
            tooltip = stringResource(Res.string.settings_window_import_upgrades_from_hacked_sov_hubs_tooltip),
            isChecked = state.isSovereigntyUpgradesHackImportingEnabled,
            onCheckedChange = viewModel::onIsSovereigntyUpgradesHackImportingEnabledClick,
        )
        RiftCheckboxWithLabel(
            label = stringResource(Res.string.settings_window_import_offline_upgrades_from_hacked_sov_hubs),
            tooltip = stringResource(Res.string.settings_window_import_offline_upgrades_from_hacked_sov_hubs_tooltip),
            isChecked = state.isSovereigntyUpgradesHackImportingOfflineEnabled,
            onCheckedChange = viewModel::onIsSovereigntyUpgradesHackImportingOfflineEnabledClick,
        )
    }
}
