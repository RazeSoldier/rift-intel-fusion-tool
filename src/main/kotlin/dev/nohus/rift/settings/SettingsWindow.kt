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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
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
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.SectionTitle
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.configurationpack.displayName
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.deleteicon
import dev.nohus.rift.generated.resources.window_settings
import dev.nohus.rift.generated.resources.window_warning
import dev.nohus.rift.notifications.NotificationEditWindow
import dev.nohus.rift.repositories.SolarSystemChipState
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.settings.SettingsViewModel.JumpBridgeCopyState
import dev.nohus.rift.settings.SettingsViewModel.JumpBridgeSearchState
import dev.nohus.rift.settings.SettingsViewModel.SettingsTab
import dev.nohus.rift.settings.SettingsViewModel.SovereigntyUpgradesCopyState
import dev.nohus.rift.settings.SettingsViewModel.UiState
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.OperatingSystem.MacOs
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.roundSecurity
import dev.nohus.rift.utils.toURIOrNull
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
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
        title = "RIFT Settings",
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
                title = "Jump Bridge Search",
                icon = Res.drawable.window_warning,
                parentState = windowState,
                state = rememberWindowState(width = 350.dp, height = Dp.Unspecified),
                onCloseClick = viewModel::onJumpBridgeDialogDismissed,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    Text(
                        text = "This feature is not unique to RIFT, and no problems were reported with it, but some " +
                                "concerns were raised that it might trip ESI's hidden rate limits and block your IP address.",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                    Text(
                        text = "Use at your own risk!",
                        textAlign = TextAlign.Center,
                        style = RiftTheme.typography.headerPrimary,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        RiftButton(
                            text = "Cancel",
                            cornerCut = ButtonCornerCut.BottomLeft,
                            type = ButtonType.Secondary,
                            onClick = viewModel::onJumpBridgeDialogDismissed,
                            modifier = Modifier.weight(1f),
                        )
                        RiftButton(
                            text = "Confirm",
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
                            OtherSettingsSection(state, viewModel)
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
            Tab(id = SettingsTab.General.id, title = "综合设置", isCloseable = false, payload = SettingsTab.General),
            Tab(id = SettingsTab.Intel.id, title = "频道", isCloseable = false, payload = SettingsTab.Intel),
            Tab(id = SettingsTab.Map.id, title = "地图", isCloseable = false, payload = SettingsTab.Map),
            Tab(id = SettingsTab.Sovereignty.id, title = "主权", isCloseable = false, payload = SettingsTab.Sovereignty),
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
    SectionTitle("用户界面", Modifier.padding(bottom = Spacing.medium))
    RiftCheckboxWithLabel(
        label = "恢复软件关闭前的窗口",
        isChecked = state.isRememberOpenWindows,
        onCheckedChange = viewModel::onRememberOpenWindowsChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "记住窗口位置",
        tooltip = "记住窗口位置\n" +
                "应用程序重启时的尺寸变化",
        isChecked = state.isRememberWindowPlacement,
        onCheckedChange = viewModel::onRememberWindowPlacementChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "使用本地时间",
        tooltip = "启用你所在的时区时间为单位显示时间的功能。" ,
        isChecked = state.isDisplayEveTime,
        onCheckedChange = viewModel::onIsDisplayEveTimeChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "使用深色托盘图标",
        isChecked = state.isUsingDarkTrayIcon,
        onCheckedChange = viewModel::onIsUsingDarkTrayIconChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "显示isk小数位",
        isChecked = state.isShowIskCents,
        onCheckedChange = viewModel::onIsShowIskCentsChanged,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    if (koin.get<OperatingSystem>() != MacOs) {
        RiftCheckboxWithLabel(
            label = "窗口智能置顶",
            tooltip = "将窗口设置在顶层\n" +
                    "仅当EVE客户端处于焦点状态时，才能位于顶层。",
            isChecked = state.isSmartAlwaysAbove,
            onCheckedChange = viewModel::onIsSmartAlwaysAboveChanged,
            modifier = Modifier.padding(bottom = Spacing.small),
        )
    }
    RiftCheckboxWithLabel(
        label = "在预警频道内显示跳跃数",
        isChecked = state.isShowingSystemDistance,
        onCheckedChange = viewModel::onIsShowingSystemDistanceChange,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "使用跳桥进行跳跃规划",
        tooltip = "允许在距离计算中包含跳桥",
        isChecked = state.isUsingJumpBridgesForDistance,
        onCheckedChange = viewModel::onIsUsingJumpBridgesForDistance,
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "启用透明窗口",
        isChecked = state.isWindowTransparencyEnabled,
        onCheckedChange = viewModel::onIsWindowTransparencyChanged,
    )
    RiftDropdownWithLabel(
        label = "窗口透明度:",
        items = listOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        selectedItem = state.windowTransparencyModifier,
        onItemSelected = viewModel::onWindowTransparencyModifierChanged,
        getItemName = {
            when (it) {
                0f -> "特别大"
                0.25f -> "大"
                0.5f -> "一般"
                0.75f -> "平"
                1f -> "谎言之镜"
                else -> "自定义"
            }
        },
    )
    RiftDropdownWithLabel(
        label = "UI 比例:",
        items = listOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f),
        selectedItem = state.uiScale,
        onItemSelected = viewModel::onUiScaleChanged,
        getItemName = { String.format("%d%%", (it * 100).toInt()) },
    )
}

@Composable
private fun AlertsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("预警", Modifier.padding(bottom = Spacing.medium))
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "选择通知位置:",
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        RiftButton(
            text = "编辑位置",
            onClick = viewModel::onEditNotificationClick,
        )
    }
    RiftSliderWithLabel(
        label = "预警音量:",
        width = 100.dp,
        range = 0..100,
        currentValue = state.soundsVolume,
        onValueChange = viewModel::onSoundsVolumeChange,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "移动端推送:",
            style = RiftTheme.typography.bodyPrimary,
            modifier = Modifier.weight(1f),
        )
        RiftButton(
            text = "Configure",
            onClick = viewModel::onConfigurePushoverClick,
        )
    }
}

@Composable
private fun OtherSettingsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("高级设置", Modifier.padding(bottom = Spacing.medium))
    RiftDropdownWithLabel(
        label = "配置包:",
        items = listOf(null) + ConfigurationPack.entries,
        selectedItem = state.configurationPack,
        onItemSelected = viewModel::onConfigurationPackChange,
        getItemName = { it?.displayName ?: "Default" },
        modifier = Modifier.padding(bottom = Spacing.small),
    )
    RiftCheckboxWithLabel(
        label = "下次启动时显示安装向导",
        isChecked = state.isShowSetupWizardOnNextStartEnabled,
        onCheckedChange = viewModel::onShowSetupWizardOnNextStartChanged,
    )
}

@Composable
private fun EveInstallationSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("EVE设置")
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "EVE日志文件夹",
            style = RiftTheme.typography.bodyPrimary,
        )
        RequirementIcon(
            isFulfilled = state.isLogsDirectoryValid,
            fulfilledTooltip = "有效的文件夹 ！",
            notFulfilledTooltip = if (state.logsDirectory.isBlank()) "该文件夹没有日志" else "无效的文件夹！",
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
            typesDescription = "聊天文件夹",
            currentPath = text,
            type = ButtonType.Secondary,
            cornerCut = ButtonCornerCut.None,
            onFileChosen = {
                text = it.absolutePathString()
                viewModel.onLogsDirectoryChanged(it.absolutePathString())
            },
        )
        RiftButton(
            text = "检测",
            type = if (state.isLogsDirectoryValid) ButtonType.Secondary else ButtonType.Primary,
            onClick = viewModel::onDetectLogsDirectoryClick,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "EVE角色文件夹",
            style = RiftTheme.typography.bodyPrimary,
        )
        RequirementIcon(
            isFulfilled = state.isSettingsDirectoryValid,
            fulfilledTooltip = "有效的文件夹",
            notFulfilledTooltip = if (state.settingsDirectory.isBlank()) "该文件夹没有设置" else "无效的文件夹！",
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
            typesDescription = "游戏日志文件夹",
            currentPath = text,
            type = ButtonType.Secondary,
            cornerCut = ButtonCornerCut.None,
            onFileChosen = {
                text = it.absolutePathString()
                viewModel.onSettingsDirectoryChanged(it.absolutePathString())
            },
        )
        RiftButton(
            text = "Detect",
            type = if (state.isSettingsDirectoryValid) ButtonType.Secondary else ButtonType.Primary,
            onClick = viewModel::onDetectSettingsDirectoryClick,
        )
    }
}

@Composable
private fun IntelChannelsSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("信息频道")
    Text(
        text = "预警信息将会在列表频道读取:",
        style = RiftTheme.typography.bodyPrimary,
        modifier = Modifier.padding(vertical = Spacing.medium),
    )
    ScrollbarColumn(
        modifier = Modifier
            .height(170.dp)
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
                            append(" – ${channel.region}")
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
                text = "无效频道",
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
            placeholder = "频道名称",
            onTextChanged = {
                addChannelText = it
            },
            modifier = Modifier.weight(1f),
        )
        val regionPlaceholder = "选择区域"
        var selectedRegion by remember { mutableStateOf(regionPlaceholder) }
        RiftDropdown(
            items = state.regions,
            selectedItem = selectedRegion,
            onItemSelected = { selectedRegion = it },
            getItemName = { it },
            maxItems = 5,
        )

        val isNameSelected = addChannelText.isNotEmpty()
        val isRegionSelected = selectedRegion != regionPlaceholder
        RiftTooltipArea(
            text = if (!isNameSelected) {
                "输入频道名"
            } else if (!isRegionSelected) {
                "为频道选择区域"
            } else {
                null
            },
        ) {
            RiftButton(
                text = "添加频道",
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
    SectionTitle("预警时效", Modifier.padding(bottom = Spacing.medium))
    val expiryItems = mapOf(
        "1 minute" to 60,
        "2 minutes" to 60 * 2,
        "5 minutes" to 60 * 5,
        "10 minutes" to 60 * 10,
        "15 minutes" to 60 * 15,
        "30 minutes" to 60 * 30,
        "1 hour" to 60 * 60,
        "永久" to Int.MAX_VALUE,
    )
    RiftDropdownWithLabel(
        label = "预警过期:",
        items = expiryItems.values.toList(),
        selectedItem = state.intelExpireSeconds,
        onItemSelected = viewModel::onIntelExpireSecondsChange,
        getItemName = { item -> expiryItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
        tooltip = """
                    选择的时间后，预警将不在地图或预警源显示。
        """.trimIndent(),
    )
}

@Composable
private fun MapUserInterfaceSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("地图用户界面", Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        RiftCheckboxWithLabel(
            label = "紧凑模式",
            isChecked = state.intelMap.isUsingCompactMode,
            onCheckedChange = viewModel::onIsUsingCompactModeChange,
        )
        RiftCheckboxWithLabel(
            label = "人物位置在可见星系实时更新",
            isChecked = state.intelMap.isFollowingCharacterWithinLayouts,
            onCheckedChange = { viewModel.onIsFollowingCharacterWithinLayoutsChange(it) },
        )
        RiftCheckboxWithLabel(
            label = "人物位置在非可见星系显示星座",
            isChecked = state.intelMap.isFollowingCharacterAcrossLayouts,
            onCheckedChange = { viewModel.onIsFollowingCharacterAcrossLayoutsChange(it) },
        )
        RiftCheckboxWithLabel(
            label = "反转滚轮缩放",
            isChecked = state.intelMap.isInvertZoom,
            onCheckedChange = { viewModel.onIsScrollZoomInvertedChange(it) },
        )
        RiftCheckboxWithLabel(
            label = "始终显示星系名称",
            isChecked = state.intelMap.isAlwaysShowingSystems,
            onCheckedChange = { viewModel.onIsAlwaysShowingSystemsChange(it) },
        )
        RiftCheckboxWithLabel(
            label = "快速加载地图星系",
            tooltip = "当点击RIFT中的某个星系时，它将\n" +
                    "在区域地图上打开，而不是在宇宙地图上打开",
            isChecked = state.intelMap.isPreferringRegionMaps,
            onCheckedChange = { viewModel.onIsPreferringRegionMapsChange(it) },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(end = Spacing.medium).fillMaxWidth(),
        ) {
            Text("查看和编辑地图标记")
            RiftButton(
                text = "地图标记",
                type = ButtonType.Primary,
                onClick = viewModel::onMapNotesClick,
            )
        }
        Text(
            text = buildAnnotatedString {
                withColor(RiftTheme.colors.textPrimary) {
                    append("小提示:")
                }
                append(" 按空格键自动调整地图大小")
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
    SectionTitle("自动导航", Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        Text(
            text = "设置自动导航时，使用：",
            style = RiftTheme.typography.bodySecondary,
        )
        RiftRadioButtonWithLabel(
            label = "RIFT规划路线",
            isChecked = state.isUsingRiftAutopilotRoute,
            onChecked = { viewModel.onIsUsingRiftAutopilotRouteChange(true) },
        )
        RiftRadioButtonWithLabel(
            label = "EVE系统规划路线",
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
    SectionTitle("预警弹窗", Modifier.padding(bottom = Spacing.medium))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
        val timeoutItems = mapOf(
            "不显示" to 0,
            "10 seconds" to 10,
            "30 seconds" to 30,
            "1 minute" to 60,
            "2 minutes" to 60 * 2,
            "5 minutes" to 60 * 5,
            "15 minutes" to 60 * 15,
            "直到关闭" to Int.MAX_VALUE,
        )
        RiftDropdownWithLabel(
            label = "弹窗时间:",
            items = timeoutItems.values.toList(),
            selectedItem = state.intelMap.intelPopupTimeoutSeconds,
            onItemSelected = viewModel::onIntelPopupTimeoutSecondsChange,
            getItemName = { item -> timeoutItems.entries.firstOrNull { it.value == item }?.key ?: "$item" },
            tooltip = """
预警弹窗时效，当有新预警时，
即使在时效之后，它们在悬停时也可见。
            """.trimIndent(),
        )
    }
}

@Composable
private fun JumpBridgeNetworkSection(
    state: UiState,
    viewModel: SettingsViewModel,
) {
    SectionTitle("跳桥", Modifier.padding())
    Column {
        val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
        ScrollbarLazyColumn(
            modifier = Modifier
                .height(140.dp)
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
                        text = "没有跳桥导入",
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
                                    Text("通过将列表复制到剪贴板来导入跳转桥")
                                    if (state.jumpBridgeNetworkUrl != null) {
                                        Text("你可以在此页面复制，粘贴:")
                                        LinkText(
                                            text = "联盟跳桥列表",
                                            onClick = { state.jumpBridgeNetworkUrl.toURIOrNull()?.openBrowser() },
                                        )
                                    } else {
                                        val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
                                        RiftTooltipArea(
                                            text = buildAnnotatedString {
                                                appendLine("支持任何格式，每行只要有两个星系名称:")
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
                                                    text = "示例",
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
                            is JumpBridgeCopyState.Copied -> {
                                val tooltip = buildString {
                                    val connections = copyState.network.take(5).joinToString("\n") {
                                        "${it.from.name} → ${it.to.name}"
                                    }
                                    append(connections)
                                    if (copyState.network.size > 5) {
                                        appendLine()
                                        append("And more…")
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
                                        Text("已复制跳桥")
                                        RiftButton(
                                            text = "Import ${copyState.network.size} connections",
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
                    Text("已与 ${network.size} 跳桥加载连接")
                    Spacer(Modifier.weight(1f))
                    RiftButton(
                        text = "复制",
                        type = ButtonType.Primary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = viewModel::onJumpBridgeCopyClick,
                        modifier = Modifier.padding(end = Spacing.medium),
                    )
                    RiftButton(
                        text = "清除",
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
                                Text("自动搜索?")
                                RiftButton(
                                    text = "搜索",
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
                        Text("搜索中 – ${String.format("%.1f", searchState.progress * 100)}%")
                        Text(
                            text = "找到 ${searchState.connectionsCount} 跳桥连接",
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
                JumpBridgeSearchState.SearchFailed -> {
                    Column(
                        modifier = Modifier.padding(top = Spacing.medium),
                    ) {
                        Text("无有效跳桥连接")
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
                            Text("找到 ${searchState.network.size} 跳桥连接")
                            RiftButton(
                                text = "Import",
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
                    label = "在地图显示跳桥",
                    isChecked = state.intelMap.isJumpBridgeNetworkShown,
                    onCheckedChange = viewModel::onIsJumpBridgeNetworkShownChange,
                )
                RiftSliderWithLabel(
                    label = "连接透明度:",
                    width = 100.dp,
                    range = 10..100,
                    currentValue = state.intelMap.jumpBridgeNetworkOpacity,
                    onValueChange = viewModel::onJumpBridgeNetworkOpacityChange,
                    getValueName = { "$it%" },
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
    SectionTitle("主权升级", Modifier.padding())
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        val solarSystemsRepository: SolarSystemsRepository = remember { koin.get() }
        ScrollbarLazyColumn(
            modifier = Modifier
                .height(140.dp)
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
                        text = "没有主权导入",
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
                                    Text("通过将列表复制到剪贴板来导入")
                                    if (state.sovereigntyUpgradesUrl != null) {
                                        Text(
                                            text = "在此页面可复制粘贴:",
                                            textAlign = TextAlign.Center,
                                        )
                                        LinkText(
                                            text = "联盟主权列表",
                                            onClick = { state.sovereigntyUpgradesUrl.toURIOrNull()?.openBrowser() },
                                        )
                                    } else {
                                        Text("每行上都需要一个星系名称和升级名称")
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
                                    Text("已复制升级")
                                    RiftButton(
                                        text = "导入 ${copyState.upgrades.size} 星系",
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
                    Text("升级 ${upgrades.size} 星系已加载")
                    Spacer(Modifier.weight(1f))
                    RiftButton(
                        text = "复制",
                        type = ButtonType.Primary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = viewModel::onSovereigntyUpgradesCopyClick,
                        modifier = Modifier.padding(end = Spacing.medium),
                    )
                    RiftButton(
                        text = "清除",
                        type = ButtonType.Negative,
                        onClick = viewModel::onSovereigntyUpgradesForgetClick,
                    )
                }
            }
        }

        RiftCheckboxWithLabel(
            label = "Import upgrades from hacked Sovereignty Hubs",
            tooltip = "Automatically import sovereignty upgrades\nwhen clicking the copy button on\na hacked Sovereignty Hub result",
            isChecked = state.isSovereigntyUpgradesHackImportingEnabled,
            onCheckedChange = viewModel::onIsSovereigntyUpgradesHackImportingEnabledClick,
        )
        RiftCheckboxWithLabel(
            label = "Import offline upgrades from hacked Sovereignty Hubs",
            tooltip = "When importing sovereignty upgrades from\na hacked Sovereignty Hub,\nalso import offline upgrades",
            isChecked = state.isSovereigntyUpgradesHackImportingOfflineEnabled,
            onCheckedChange = viewModel::onIsSovereigntyUpgradesHackImportingOfflineEnabledClick,
        )
    }
}
