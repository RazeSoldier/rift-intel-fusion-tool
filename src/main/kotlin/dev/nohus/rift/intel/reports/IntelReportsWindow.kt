package dev.nohus.rift.intel.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.ChatMessage
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.ContextMenuItem.CheckboxItem
import dev.nohus.rift.compose.ContextMenuItem.HeaderItem
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftSearchField
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.TitleBarStyle
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.*
import dev.nohus.rift.i18n.getStringSync
import dev.nohus.rift.intel.ParsedChannelChatMessage
import dev.nohus.rift.intel.reports.IntelReportsSettings
import dev.nohus.rift.intel.reports.IntelReportsViewModel.UiState
import dev.nohus.rift.intel.state.AlertTriggeringMessagesRepository.AlertTriggeringMessage
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.stringResource
import java.time.Instant

@Composable
fun IntelReportsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: IntelReportsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = stringResource(Res.string.intel_reports_window_title),
        icon = Res.drawable.window_bleedchannel,
        state = windowState,
        tuneContextMenuItems = getTuneContextMenuItems(state, viewModel),
        onCloseClick = onCloseRequest,
        titleBarStyle = if (state.settings.isUsingCompactMode) TitleBarStyle.Small else TitleBarStyle.Full,
        withContentPadding = false,
    ) {
        IntelReportsWindowContent(
            state = state,
            onIntelChannelFilterSelect = viewModel::onIntelChannelFilterSelect,
            onSearchChange = viewModel::onSearchChange,
        )
    }
}

private fun getTuneContextMenuItems(
    state: UiState,
    viewModel: IntelReportsViewModel,
): List<ContextMenuItem>? {
    val isUsingCompactMode = state.settings.isUsingCompactMode
    val isUsingReverseOrder = state.settings.isUsingReverseOrder
    val isShowingReporter = state.settings.isShowingReporter
    val isShowingChannel = state.settings.isShowingChannel
    val isShowingRegion = state.settings.isShowingRegion
    return buildList {
        add(HeaderItem(getStringSync(Res.string.intel_reports_user_interface)))
        add(CheckboxItem(getStringSync(Res.string.compact_mode), isSelected = isUsingCompactMode, onClick = { viewModel.onIsUsingCompactModeChange(!isUsingCompactMode) }))
        add(CheckboxItem(getStringSync(Res.string.intel_reports_show_newest_on_top), isSelected = isUsingReverseOrder, onClick = { viewModel.onIsUsingReverseOrderChange(!isUsingReverseOrder) }))
        add(HeaderItem(getStringSync(Res.string.intel_reports_shown_info)))
        add(CheckboxItem(getStringSync(Res.string.intel_reports_show_reporter_name), isSelected = isShowingReporter, onClick = { viewModel.onIsShowingReporterChange(!isShowingReporter) }))
        add(CheckboxItem(getStringSync(Res.string.intel_reports_show_channel_name), isSelected = isShowingChannel, onClick = { viewModel.onIsShowingChannelChange(!isShowingChannel) }))
        add(CheckboxItem(getStringSync(Res.string.intel_reports_show_channel_region), isSelected = isShowingRegion, onClick = { viewModel.onIsShowingRegionChange(!isShowingRegion) }))
    }.takeIf { it.isNotEmpty() }
}

@Composable
private fun IntelReportsWindowContent(
    state: UiState,
    onIntelChannelFilterSelect: (String) -> Unit,
    onSearchChange: (String) -> Unit,
) {
    val outerPadding = if (state.settings.isUsingCompactMode) Spacing.medium else Spacing.large
    Column {
        FiltersRow(
            padding = outerPadding,
            state = state,
            onIntelChannelFilterSelect = onIntelChannelFilterSelect,
            onSearchChange = onSearchChange,
        )
        ScrollingIntelPanel(
            settings = state.settings,
            channelChatMessages = state.channelChatMessages,
            alertTriggeringMessages = state.alertTriggeringMessages,
            padding = outerPadding,
        )
        if (state.channelChatMessages.isEmpty()) {
            val text = if (state.intelChannels.isEmpty()) {
                stringResource(Res.string.intel_reports_intel_channels_no_configured)
            } else if (state.hasOnlineCharacters) {
                if (state.search != null) {
                    stringResource(Res.string.intel_reports_intel_not_found)
                } else if (state.filteredChannel != null) {
                    stringResource(Res.string.intel_reports_intel_not_found_in_channel, state.filteredChannel.name)
                } else {
                    stringResource(Res.string.intel_reports_no_intel_received)
                }
            } else {
                stringResource(Res.string.intel_reports_no_online_characters)
            }
            Text(
                text = text,
                style = RiftTheme.typography.headerPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.large),
            )
        }
    }
}

@Composable
private fun FiltersRow(
    padding: Dp,
    state: UiState,
    onIntelChannelFilterSelect: (String) -> Unit,
    onSearchChange: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = padding, end = padding, bottom = Spacing.medium),
    ) {
        val allChannelsOption = stringResource(Res.string.intel_reports_all_channels)
        RiftDropdownWithLabel(
            label = stringResource(Res.string.intel_reports_filter),
            items = listOf(allChannelsOption) + state.intelChannels.map { it.name }.toSet(),
            selectedItem = state.filteredChannel?.name ?: allChannelsOption,
            onItemSelected = onIntelChannelFilterSelect,
            getItemName = { it },
            height = if (state.settings.isUsingCompactMode) 24.dp else 32.dp,
        )
        Spacer(Modifier.weight(1f))
        RiftSearchField(
            search = state.search,
            isCompact = state.settings.isUsingCompactMode,
            onSearchChange = onSearchChange,
            modifier = Modifier.padding(start = Spacing.medium),
        )
    }
}

@Composable
private fun ScrollingIntelPanel(
    settings: IntelReportsSettings,
    channelChatMessages: List<ParsedChannelChatMessage>,
    alertTriggeringMessages: List<AlertTriggeringMessage>,
    padding: Dp,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(channelChatMessages) {
        if (settings.isUsingReverseOrder) {
            if (channelChatMessages.isNotEmpty()) {
                listState.scrollToItem(0)
            }
        } else {
            channelChatMessages.lastIndex.takeIf { it > -1 }?.let {
                listState.scrollToItem(it)
            }
        }
    }

    val enterAnimations: MutableMap<Instant, Animatable<Float, AnimationVector1D>> = remember { mutableStateMapOf() }
    ScrollbarLazyColumn(
        listState = listState,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(start = padding, bottom = padding),
        scrollbarModifier = Modifier.padding(end = padding / 2),
    ) {
        items(channelChatMessages) { message ->
            val alertTriggerTimestamp = alertTriggeringMessages.firstOrNull { it.message == message }?.alertTriggerTimestamp
            ChatMessage(
                settings = settings,
                message = message,
                alertTriggerTimestamp = alertTriggerTimestamp,
                enterAnimation = enterAnimations.getOrPut(message.chatMessage.timestamp) { Animatable(0f) },
            )
        }
    }
}
