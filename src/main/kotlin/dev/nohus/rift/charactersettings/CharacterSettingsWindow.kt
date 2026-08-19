package dev.nohus.rift.charactersettings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.charactersettings.CharacterSettingsViewModel.CharacterItem
import dev.nohus.rift.charactersettings.CharacterSettingsViewModel.CopyingState
import dev.nohus.rift.charactersettings.CharacterSettingsViewModel.CopySetting
import dev.nohus.rift.charactersettings.CharacterSettingsViewModel.UiState
import dev.nohus.rift.charactersettings.compose.ChatChannels
import dev.nohus.rift.charactersettings.compose.EveResourceIcon
import dev.nohus.rift.charactersettings.compose.NeocomButtonDefinitions
import dev.nohus.rift.charactersettings.compose.NeocomButtons
import dev.nohus.rift.charactersettings.compose.ProbeFormations
import dev.nohus.rift.charactersettings.compose.ShipState
import dev.nohus.rift.charactersettings.compose.ShipStateToggle
import dev.nohus.rift.charactersettings.compose.WindowLayoutPreview
import dev.nohus.rift.charactersettings.compose.Watchlist
import dev.nohus.rift.charactersettings.io.ReadAccountSettingsUseCase
import dev.nohus.rift.charactersettings.io.ReadCharacterSettingsUseCase.NeocomButton
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckboxWithLabel
import dev.nohus.rift.compose.RiftContextMenuArea
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftSideNavigation
import dev.nohus.rift.compose.RiftSideNavigationHeader
import dev.nohus.rift.compose.RiftSideNavigationItem
import dev.nohus.rift.compose.RiftSlider
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftTabBar
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWarningBanner
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.Tab
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.getRelativeTime
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.copy_16px
import dev.nohus.rift.generated.resources.recall_drones_16px
import dev.nohus.rift.generated.resources.window_character_settings
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.utils.withStyle
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource
import java.time.ZoneId

@Composable
fun CharacterSettingsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: CharacterSettingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Character Settings Management",
        icon = Res.drawable.window_character_settings,
        state = windowState,
        onCloseClick = onCloseRequest,
        withContentPadding = false,
    ) {
        CharacterSettingsWindowContent(
            state = state,
            onCharacterSelected = viewModel::onCharacterSelected,
            onAssignAccount = viewModel::onAssignAccount,
            onCopySource = viewModel::onCopySourceClick,
            onProfileSelected = viewModel::onProfileSelected,
            onCopyDestination = viewModel::onCopyDestinationClick,
            onCopySettingChanged = viewModel::onCopySettingChanged,
            onCopySettingsSelected = viewModel::onCopySettingsSelected,
            onCopyConfirm = viewModel::onCopySettingsConfirmClick,
            onCancelCopy = viewModel::onCancelCopy,
            onWatchlistCharacterNameChanged = viewModel::onWatchlistCharacterNameChanged,
            onWatchlistUpdated = viewModel::onWatchlistUpdated,
            onWatchlistCharacterRemoved = viewModel::onWatchlistCharacterRemoved,
            onNeocomButtonColorChanged = viewModel::onNeocomButtonColorChanged,
            onNeocomButtonsReordered = viewModel::onNeocomButtonsReordered,
            onNeocomButtonAdded = viewModel::onNeocomButtonAdded,
            onNeocomButtonRemoved = viewModel::onNeocomButtonRemoved,
            onNeocomWidthChanged = viewModel::onNeocomWidthChanged,
            onWindowBoundsChanged = viewModel::onWindowBoundsChanged,
            onWindowDragStarted = viewModel::onWindowDragStarted,
            onWindowMinimizedChanged = viewModel::onWindowMinimizedChanged,
            onWindowOpened = viewModel::onWindowOpened,
            onWindowClosed = viewModel::onWindowClosed,
            onShipUiOffsetChanged = viewModel::onShipUiOffsetChanged,
            onShipUiVerticalPositionChanged = viewModel::onShipUiVerticalPositionChanged,
            onTargetOriginChanged = viewModel::onTargetOriginChanged,
            onTargetsAlignmentChanged = viewModel::onTargetsAlignmentChanged,
            onChatChannelsChanged = viewModel::onChatChannelsChanged,
            onProbeFormationsChanged = viewModel::onProbeFormationsChanged,
            onRevertChanges = viewModel::onRevertChanges,
            onSaveChanges = viewModel::onSaveChanges,
            onBackupSettings = viewModel::onBackupSettingsClick,
            onDeleteBackup = viewModel::onDeleteBackupClick,
        )

        state.backupDialogCharacterId?.let {
            BackupNameDialog(
                parentWindowState = windowState,
                onDismiss = viewModel::onBackupDialogDismissed,
                onConfirm = viewModel::onCreateBackup,
            )
        }

        state.deleteBackupDialogCharacterId?.let { settingsId ->
            state.characters.firstOrNull { it.settingsId == settingsId }?.let { backup ->
                DeleteBackupDialog(
                    parentWindowState = windowState,
                    backup = backup,
                    onDismiss = viewModel::onDeleteBackupDialogDismissed,
                    onConfirm = viewModel::onDeleteBackupConfirm,
                )
            }
        }

        state.dialogMessage?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }
    }
}

@Composable
private fun WindowScope.CharacterSettingsWindowContent(
    state: UiState,
    onCharacterSelected: (Int) -> Unit,
    onAssignAccount: (characterId: Int, accountId: Int) -> Unit,
    onCopySource: (Int) -> Unit,
    onProfileSelected: (Int, String) -> Unit,
    onCopyDestination: (Int) -> Unit,
    onCopySettingChanged: (CopySetting?, Boolean) -> Unit,
    onCopySettingsSelected: () -> Unit,
    onCopyConfirm: () -> Unit,
    onCancelCopy: () -> Unit,
    onWatchlistCharacterNameChanged: (String) -> Unit,
    onWatchlistUpdated: (Int, Color?) -> Unit,
    onWatchlistCharacterRemoved: (Int) -> Unit,
    onNeocomButtonColorChanged: (String, Int?) -> Unit,
    onNeocomButtonsReordered: (String?, Int, Int) -> Unit,
    onNeocomButtonAdded: (String) -> Unit,
    onNeocomButtonRemoved: (String) -> Unit,
    onNeocomWidthChanged: (Int) -> Unit,
    onWindowBoundsChanged: (String, Int, Int, Int, Int) -> Unit,
    onWindowDragStarted: (String) -> Unit,
    onWindowMinimizedChanged: (String) -> Unit,
    onWindowOpened: (String) -> Unit,
    onWindowClosed: (String) -> Unit,
    onShipUiOffsetChanged: (Float) -> Unit,
    onShipUiVerticalPositionChanged: () -> Unit,
    onTargetOriginChanged: (Pair<Float, Float>) -> Unit,
    onTargetsAlignmentChanged: () -> Unit,
    onChatChannelsChanged: (Map<String, String>) -> Unit,
    onProbeFormationsChanged: (List<ReadAccountSettingsUseCase.ProbeFormation>) -> Unit,
    onRevertChanges: (Int) -> Unit,
    onSaveChanges: (Int) -> Unit,
    onBackupSettings: (Int) -> Unit,
    onDeleteBackup: (Int) -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        SideNavigation(state, onCharacterSelected)
        Column {
            CopyControls(
                state = state,
                onCopySettingChanged = onCopySettingChanged,
                onCopySettingsSelected = onCopySettingsSelected,
                onCopyConfirm = onCopyConfirm,
                onCancelCopy = onCancelCopy,
            )
            state.selectedCharacterId
                ?.let { characterId -> state.characters.firstOrNull { it.settingsId == characterId } }
                ?.let { character ->
                    CharacterPage(
                        state = state,
                        character = character,
                        onAssignAccount = onAssignAccount,
                        onProfileSelected = onProfileSelected,
                        onCopySource = onCopySource,
                        onCopyDestination = onCopyDestination,
                        watchlistCharacterName = state.watchlistCharacterName,
                        watchlistCharacterId = state.watchlistCharacterId,
                        isWatchlistCharacterLookupComplete = state.isWatchlistCharacterLookupComplete,
                        onWatchlistCharacterNameChanged = onWatchlistCharacterNameChanged,
                        onWatchlistUpdated = onWatchlistUpdated,
                        onWatchlistCharacterRemoved = onWatchlistCharacterRemoved,
                        onNeocomButtonColorChanged = onNeocomButtonColorChanged,
                        onNeocomButtonsReordered = onNeocomButtonsReordered,
                        onNeocomButtonAdded = onNeocomButtonAdded,
                        onNeocomButtonRemoved = onNeocomButtonRemoved,
                        onNeocomWidthChanged = onNeocomWidthChanged,
                        onWindowBoundsChanged = onWindowBoundsChanged,
                        onWindowDragStarted = onWindowDragStarted,
                        onWindowMinimizedChanged = onWindowMinimizedChanged,
                        onWindowOpened = onWindowOpened,
                        onWindowClosed = onWindowClosed,
                        onShipUiOffsetChanged = onShipUiOffsetChanged,
                        onShipUiVerticalPositionChanged = onShipUiVerticalPositionChanged,
                        onTargetOriginChanged = onTargetOriginChanged,
                        onTargetsAlignmentChanged = onTargetsAlignmentChanged,
                        onChatChannelsChanged = onChatChannelsChanged,
                        onProbeFormationsChanged = onProbeFormationsChanged,
                        onRevertChanges = onRevertChanges,
                        onSaveChanges = onSaveChanges,
                        onBackupSettings = onBackupSettings,
                        onDeleteBackup = onDeleteBackup,
                    )
                } ?: EmptyCharacterPage()
        }
    }
}

@Composable
private fun SideNavigation(
    state: UiState,
    onCharacterSelected: (Int) -> Unit
) {
    RiftSideNavigation(
        footer = {}
    ) {
        state.accounts.sortedByDescending { it.lastModified }.forEach { account ->
            val characters = state.characters
                .filter { it.backupName == null && it.accountId == account.id }
                .sortedBy { it.characterId }
            if (characters.isNotEmpty()) {
                item {
                    RiftTooltipArea(
                        tooltip = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                modifier = Modifier
                                    .widthIn(max = 400.dp)
                                    .padding(Spacing.large)
                            ) {
                                val now = getNow()
                                Text(
                                    text = buildAnnotatedString {
                                        append("Last login: ")
                                        withColor(RiftTheme.colors.textPrimary) {
                                            append(getRelativeTime(account.lastModified, ZoneId.systemDefault(), now))
                                        }
                                    },
                                    style = RiftTheme.typography.bodySecondary
                                )
                                Text(
                                    text = "Settings files:",
                                    style = RiftTheme.typography.bodySecondary
                                )
                                account.paths.values.forEach { path ->
                                    Text(
                                        text = path.toString(),
                                        style = RiftTheme.typography.detailPrimary
                                    )
                                }
                            }
                        }
                    ) {
                        RiftSideNavigationHeader("Account ${account.id}")
                    }
                }
                characters.forEach { character ->
                    CharacterItem(character, state.selectedCharacterId, onCharacterSelected)
                }
            }
        }

        val unassigned = state.characters.filter { it.backupName == null && it.accountId == null }.sortedBy { it.characterId }
        if (unassigned.isNotEmpty()) {
            item {
                RiftSideNavigationHeader("Unassigned to accounts")
            }
            unassigned.forEach {
                CharacterItem(it, state.selectedCharacterId, onCharacterSelected)
            }
        }

        item {
            RiftSideNavigationHeader("Backups")
        }
        val backups = state.characters
            .filter { it.backupName != null }
            .sortedByDescending { it.backupTimestamp }
        if (backups.isEmpty()) {
            item {
                RiftSideNavigationItem(
                    text = "No backups yet",
                    count = null,
                    icon = null,
                    isSelected = false,
                    onClick = {},
                )
            }
        } else {
            backups.forEach { CharacterItem(it, state.selectedCharacterId, onCharacterSelected) }
        }
    }
}

private fun LazyListScope.CharacterItem(
    character: CharacterItem,
    selectedCharacterId: Int?,
    onCharacterSelected: (Int) -> Unit,
) {
    item {
        RiftSideNavigationItem(
            text = if (character.backupName != null) {
                "${character.info?.name ?: character.characterId} - ${character.backupName}"
            } else {
                character.info?.name ?: "Could not load"
            },
            count = null,
            icon = {
                DynamicCharacterPortraitParallax(
                    characterId = character.characterId,
                    size = 32.dp,
                    enterTimestamp = null,
                    pointerInteractionStateHolder = null
                )
            },
            isSelected = selectedCharacterId == character.settingsId,
            onClick = { onCharacterSelected(character.settingsId) }
        )
    }
}

@Composable
private fun WindowScope.CharacterPage(
    state: UiState,
    character: CharacterItem,
    onAssignAccount: (characterId: Int, accountId: Int) -> Unit,
    onProfileSelected: (Int, String) -> Unit,
    onCopySource: (Int) -> Unit,
    onCopyDestination: (Int) -> Unit,
    watchlistCharacterName: String,
    watchlistCharacterId: Int?,
    isWatchlistCharacterLookupComplete: Boolean,
    onWatchlistCharacterNameChanged: (String) -> Unit,
    onWatchlistUpdated: (Int, Color?) -> Unit,
    onWatchlistCharacterRemoved: (Int) -> Unit,
    onNeocomButtonColorChanged: (String, Int?) -> Unit,
    onNeocomButtonsReordered: (String?, Int, Int) -> Unit,
    onNeocomButtonAdded: (String) -> Unit,
    onNeocomButtonRemoved: (String) -> Unit,
    onNeocomWidthChanged: (Int) -> Unit,
    onWindowBoundsChanged: (String, Int, Int, Int, Int) -> Unit,
    onWindowDragStarted: (String) -> Unit,
    onWindowMinimizedChanged: (String) -> Unit,
    onWindowOpened: (String) -> Unit,
    onWindowClosed: (String) -> Unit,
    onShipUiOffsetChanged: (Float) -> Unit,
    onShipUiVerticalPositionChanged: () -> Unit,
    onTargetOriginChanged: (Pair<Float, Float>) -> Unit,
    onTargetsAlignmentChanged: () -> Unit,
    onChatChannelsChanged: (Map<String, String>) -> Unit,
    onProbeFormationsChanged: (List<ReadAccountSettingsUseCase.ProbeFormation>) -> Unit,
    onRevertChanges: (Int) -> Unit,
    onSaveChanges: (Int) -> Unit,
    onBackupSettings: (Int) -> Unit,
    onDeleteBackup: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = Spacing.large, end = Spacing.large, bottom = Spacing.large)
    ) {
        if (state.isOnline) {
            RiftWarningBanner("You are online. Settings can't be copied between characters while the game is open. Make sure to close EVE first.")
        }
        if (
            character.backupName == null &&
            character.accountId != null &&
            state.characters.count { it.backupName == null && it.accountId == character.accountId } > 3
        ) {
            RiftWarningBanner("This account has more than 3 characters assigned, which is not possible. Correct the assignment.")
        }

        val profiles = if (character.backupName != null) character.settingsFiles.keys.sorted() else state.profiles
        val selectedProfile = state.selectedProfiles[character.settingsId]?.takeIf { it in profiles }

        CharacterPageHeader(
            state = state,
            character = character,
            selectedProfile = selectedProfile,
            profiles = profiles,
            onProfileSelected = onProfileSelected,
            onAssignAccount = onAssignAccount,
            onCopySource = onCopySource,
            onCopyDestination = onCopyDestination,
            onRevertChanges = onRevertChanges,
            onSaveChanges = onSaveChanges,
            onBackupSettings = onBackupSettings,
            onDeleteBackup = onDeleteBackup,
        )

        val allSettings = state.displayedSettings[character.settingsId]
        val characterSettings = allSettings?.characterSettings
        val accountSettings = allSettings?.accountSettings

        if (selectedProfile == null || selectedProfile !in character.settingsFiles) {
            Text(
                text = if (selectedProfile == null) {
                    "No launcher profiles with EVE settings were found."
                } else {
                    "This character has no settings in the “$selectedProfile” launcher profile. You can paste settings here from another character or profile."
                },
                style = RiftTheme.typography.bodySecondary,
                modifier = Modifier.padding(top = Spacing.large),
            )
            return
        }

        var selectedTab by remember { mutableStateOf(TAB_WINDOW_LAYOUT) }
        RiftTabBar(
            tabs = listOf(
                Tab(id = TAB_WINDOW_LAYOUT, title = "Window Layout", isCloseable = false),
                Tab(id = TAB_NEOCOM, title = "Neocom Buttons", isCloseable = false),
                Tab(id = TAB_WATCHLIST, title = "Fleet Watchlist", isCloseable = false),
                Tab(id = TAB_CHAT_CHANNELS, title = "Chat Channels", isCloseable = false),
                Tab(id = TAB_PROBE_FORMATIONS, title = "Probe Formations", isCloseable = false),
            ),
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            onTabClosed = {}
        )

        when (selectedTab) {
            TAB_WINDOW_LAYOUT -> {
                Text(
                    text = "You can drag and resize windows, minimize, close or add new windows to your layout. The ship capacitor and locked targets are also editable.",
                    style = RiftTheme.typography.bodySecondary,
                )

                var shipState by remember { mutableStateOf(ShipState.InSpace) }
                var isShowingFleet by remember { mutableStateOf(true) }
                var isShowingDrones by remember { mutableStateOf(true) }
                var isShowingMinimized by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    ShipStateToggle(shipState, onChange = { shipState = it })
                    Text(
                        text = "Show:",
                        style = RiftTheme.typography.bodyPrimary
                    )
                    RiftCheckboxWithLabel(
                        label = "Fleet",
                        tooltip = "Show the Fleet and Watchlist windows.\nIn-game they only appear when you are in a fleet.",
                        isChecked = isShowingFleet,
                        onCheckedChange = { isShowingFleet = it }
                    )
                    RiftCheckboxWithLabel(
                        label = "Drones",
                        tooltip = "Show the Drones window.\nIt will only show in the In Space layout.\nIn-game it only appears when you have drones.",
                        isChecked = isShowingDrones,
                        onCheckedChange = { isShowingDrones = it }
                    )
                    RiftCheckboxWithLabel(
                        label = "Minimized",
                        tooltip = "Show minimized windows.\nThese windows are still part of your window layout, but are saved minimized.",
                        isChecked = isShowingMinimized,
                        onCheckedChange = { isShowingMinimized = it }
                    )
                    val categorizeWindowUseCase: CategorizeWindowUseCase = remember { koin.get() }
                    RiftContextMenuArea(
                        items = categorizeWindowUseCase.persistentWindows
                            .filter { (windowId, window) ->
                                window.isPersistent &&
                                    window.isOpenStateControlled &&
                                    window.isAddable &&
                                    windowId !in characterSettings?.openWindows.orEmpty()
                            }
                            .toList()
                            .sortedBy { it.second.name }
                            .map { (windowId, window) ->
                                ContextMenuItem.TextItem(
                                    text = window.name,
                                    iconContent = window.icon?.let { icon ->
                                        { _ ->
                                            Image(
                                                painter = painterResource(icon),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    },
                                    onClick = { onWindowOpened(windowId) },
                                )
                            },
                        acceptsLeftClick = true,
                        acceptsRightClick = false,
                    ) {
                        RiftButton(
                            text = "Add window",
                            onClick = {},
                        )
                    }
                }
                WindowLayoutPreview(
                    selectedProfile = selectedProfile,
                    settings = characterSettings,
                    shipState = shipState,
                    isShowingFleet = isShowingFleet,
                    isShowingDrones = isShowingDrones,
                    isShowingMinimized = isShowingMinimized,
                    accountSettings = accountSettings,
                    windowLayerOrder = state.windowLayerOrders[character.settingsId].orEmpty(),
                    onWindowBoundsChanged = onWindowBoundsChanged,
                    onWindowDragStarted = onWindowDragStarted,
                    onWindowMinimizedChanged = onWindowMinimizedChanged,
                    onWindowClosed = onWindowClosed,
                    onShipUiOffsetChanged = onShipUiOffsetChanged,
                    onShipUiVerticalPositionChanged = onShipUiVerticalPositionChanged,
                    onTargetOriginChanged = onTargetOriginChanged,
                    onTargetsAlignmentChanged = onTargetsAlignmentChanged,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }
            TAB_NEOCOM -> {
                if (characterSettings != null) {
                    ScrollbarColumn(
                        contentPadding = PaddingValues(end = Spacing.medium),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            modifier = Modifier.padding(bottom = Spacing.medium),
                        ) {
                            Text(
                                text = "You can resize your Neocom, or add, remove, reorder, and color its buttons.",
                                style = RiftTheme.typography.bodySecondary,
                            )
                            Column {
                                Text(
                                    text = buildAnnotatedString {
                                        append("Neocom width: ")
                                        withStyle(RiftTheme.colors.textSecondary) {
                                            append("${characterSettings.neocomWidthPx}px")
                                        }
                                    },
                                    style = RiftTheme.typography.bodyPrimary,
                                )
                                RiftSlider(
                                    width = 200.dp,
                                    range = NEOCOM_WIDTH_RANGE,
                                    currentValue = characterSettings.neocomWidthPx,
                                    onValueChange = onNeocomWidthChanged,
                                    getValueName = { "${it}px" },
                                    isPreciseScroll = true,
                                    isImmediate = true,
                                )
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(with(LocalDensity.current) { characterSettings.neocomWidthPx.toDp() })
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .border(1.dp, RiftTheme.colors.borderGreyLight)
                                    .padding(with(LocalDensity.current) { 4.toDp() })
                            ) {
                                characterSettings.neocomButtons.firstOrNull()?.let { button ->
                                    EveResourceIcon(
                                        resourcePath = button.iconPath,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }

                            val buttonIds = characterSettings.neocomButtons.getNeocomButtonIds()
                            RiftContextMenuArea(
                                items = NeocomButtonDefinitions
                                    .filter { it.isAddable && it.id !in buttonIds }
                                    .sortedBy { it.name }
                                    .map { button ->
                                        ContextMenuItem.TextItem(
                                            text = button.name,
                                            iconContent = { _ ->
                                                EveResourceIcon(
                                                    resourcePath = button.iconPath,
                                                    modifier = Modifier.size(24.dp),
                                                )
                                            },
                                            onClick = { onNeocomButtonAdded(button.id) },
                                        )
                                    },
                                acceptsLeftClick = true,
                                acceptsRightClick = false,
                            ) {
                                RiftButton(
                                    text = "Add button",
                                    onClick = {},
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.small))
                        NeocomButtons(
                            buttons = characterSettings.neocomButtons,
                            parentId = null,
                            onButtonColorChanged = onNeocomButtonColorChanged,
                            onButtonsReordered = onNeocomButtonsReordered,
                            onButtonRemoved = onNeocomButtonRemoved,
                        )
                    }
                }
            }
            TAB_WATCHLIST -> {
                if (characterSettings != null) {
                    Watchlist(
                        settings = characterSettings,
                        characterName = watchlistCharacterName,
                        characterId = watchlistCharacterId,
                        isCharacterLookupComplete = isWatchlistCharacterLookupComplete,
                        onCharacterNameChanged = onWatchlistCharacterNameChanged,
                        onWatchlistUpdated = onWatchlistUpdated,
                        onWatchlistCharacterRemoved = onWatchlistCharacterRemoved,
                    )
                }
            }
            TAB_CHAT_CHANNELS -> {
                if (characterSettings != null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(
                            text = "Here are the chat channels you are in. You can leave channels below.",
                            style = RiftTheme.typography.bodySecondary,
                        )
                        ChatChannels(characterSettings.joinedChatChannels, onChatChannelsChanged)
                    }
                }
            }
            TAB_PROBE_FORMATIONS -> {
                ProbeFormations(accountSettings?.probeFormations.orEmpty(), onProbeFormationsChanged)
            }
        }
    }
}

private const val TAB_WINDOW_LAYOUT = 0
private const val TAB_NEOCOM = 1
private const val TAB_WATCHLIST = 2
private const val TAB_CHAT_CHANNELS = 3
private const val TAB_PROBE_FORMATIONS = 4
private val NEOCOM_WIDTH_RANGE = 24..72

private fun List<NeocomButton>.getNeocomButtonIds(): Set<String> {
    return flatMapTo(mutableSetOf()) { button -> setOf(button.id) + button.children.getNeocomButtonIds() }
}

@Composable
private fun AccountAssignmentControls(
    character: CharacterItem,
    state: UiState,
    onAssignAccount: (Int, Int) -> Unit
) {
    var isAssigningAccount: Boolean by remember(character) { mutableStateOf(false) }

    AnimatedContent(isAssigningAccount) { isAssigning ->
        if (isAssigning) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Assign this character to an account:",
                    style = RiftTheme.typography.bodyPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small)
                ) {
                    state.accounts.forEach { account ->
                        RiftButton(
                            "Account ${account.id}",
                            isCompact = true,
                            onClick = {
                                onAssignAccount(character.characterId, account.id)
                                isAssigningAccount = false
                            }
                        )
                    }
                }
            }
        } else {
            if (character.accountId != null && state.accounts.size > 1) {
                RiftTooltipArea(
                    text = "If the character is assigned to an incorrect account,\nyou can change it here"
                ) {
                    RiftButton(
                        text = "Reassign account",
                        type = ButtonType.Secondary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = { isAssigningAccount = true },
                    )
                }
            } else if (character.accountId == null) {
                Text(
                    text = "This character is not associated with an account. Assign below or log in to the character to automatically assign.",
                    style = RiftTheme.typography.bodyPrimary
                )
                RiftButton(
                    text = "Assign account",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = { isAssigningAccount = true },
                )
            }
        }
    }
}

@Composable
private fun CharacterPageHeader(
    state: UiState,
    character: CharacterItem,
    selectedProfile: String?,
    profiles: List<String>,
    onProfileSelected: (Int, String) -> Unit,
    onAssignAccount: (characterId: Int, accountId: Int) -> Unit,
    onCopySource: (Int) -> Unit,
    onCopyDestination: (Int) -> Unit,
    onRevertChanges: (Int) -> Unit,
    onSaveChanges: (Int) -> Unit,
    onBackupSettings: (Int) -> Unit,
    onDeleteBackup: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        DynamicCharacterPortraitParallax(
            characterId = character.characterId,
            size = 64.dp,
            enterTimestamp = null,
            pointerInteractionStateHolder = null
        )

        Column(
            modifier = Modifier.padding(horizontal = Spacing.medium)
        ) {
            if (character.backupName != null) {
                Text(
                    text = "${character.info?.name ?: character.characterId} - ${character.backupName}",
                    style = RiftTheme.typography.headerHighlighted,
                )
                character.backupTimestamp?.let { timestamp ->
                    Text(
                        text = "Backed up ${getRelativeTime(timestamp, ZoneId.systemDefault(), getNow())}",
                        style = RiftTheme.typography.bodySecondary,
                    )
                }
            } else if (character.info != null) {
                Text(
                    text = character.info.name,
                    style = RiftTheme.typography.headerHighlighted,
                )
            } else {
                Text(
                    text = "Could not load",
                    style = RiftTheme.typography.bodySecondary.copy(color = RiftTheme.colors.borderError),
                )
            }

            if (selectedProfile != null) {
                Text(
                    text = "Launcher profile",
                    style = RiftTheme.typography.bodySecondary
                )
                if (character.backupName != null) {
                    Text(
                        text = selectedProfile,
                        style = RiftTheme.typography.bodyPrimary,
                    )
                } else {
                    RiftDropdown(
                        items = profiles,
                        selectedItem = selectedProfile,
                        onItemSelected = { onProfileSelected(character.settingsId, it) },
                        getItemName = { it },
                    )
                }
            } else {
                RequirementIcon(
                    isFulfilled = false,
                    fulfilledTooltip = "",
                    notFulfilledTooltip = "EVE settings file for this character is missing.\nMake sure you have logged in to the game at least once.",
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.padding(start = Spacing.medium)
        ) {
            if (character.backupName == null) {
                AccountAssignmentControls(
                    character = character,
                    state = state,
                    onAssignAccount = onAssignAccount
                )
            }

            CharacterCopyControls(
                state = state,
                character = character,
                onCopySource = onCopySource,
                onCopyDestination = onCopyDestination,
                onRevertChanges = onRevertChanges,
                onSaveChanges = onSaveChanges,
                onBackupSettings = onBackupSettings,
                onDeleteBackup = onDeleteBackup,
            )
        }
    }
}

@Composable
private fun CopyControls(
    state: UiState,
    onCopySettingChanged: (CopySetting?, Boolean) -> Unit,
    onCopySettingsSelected: () -> Unit,
    onCopyConfirm: () -> Unit,
    onCancelCopy: () -> Unit
) {
    AnimatedContent(state.copying, contentKey = {it == CopyingState.SelectingSource }) {
        when (state.copying) {
            CopyingState.SelectingSource -> {}
            else -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier.padding(start = Spacing.large, end = Spacing.large, bottom = Spacing.large)
                ) {
                    when (val copying = state.copying) {
                        is CopyingState.SelectingSettings -> {
                            Text(
                                text = buildAnnotatedString {
                                    append("Copying settings from ")
                                    withColor(RiftTheme.colors.textHighlighted) { append(copying.source.name) }
                                },
                                style = RiftTheme.typography.headerPrimary,
                            )
                            Text("Choose which settings to copy:", style = RiftTheme.typography.bodyPrimary)
                            FlowRow(
                                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                            ) {
                                CopySetting.entries.forEach { setting ->
                                    RiftCheckboxWithLabel(
                                        label = setting.displayName.replaceFirstChar { it.uppercase() },
                                        isChecked = setting in copying.settings,
                                        onCheckedChange = { onCopySettingChanged(setting, it) },
                                    )
                                }
                                RiftCheckboxWithLabel(
                                    label = "All settings",
                                    tooltip = "Completely replace the settings files, including settings not editable in RIFT",
                                    isChecked = copying.allSettings,
                                    onCheckedChange = { onCopySettingChanged(null, it) },
                                )
                            }
                        }
                        is CopyingState.SelectingDestination -> {
                            Text(
                                text = buildAnnotatedString {
                                    append("Copying ${copying.settingsDescription()} from ")
                                    withColor(RiftTheme.colors.textHighlighted) {
                                        append(copying.source.name)
                                    }
                                },
                                style = RiftTheme.typography.headerPrimary
                            )
                            Text(
                                text = "Choose characters to paste these settings to from their character pages.",
                                style = RiftTheme.typography.bodyPrimary
                            )
                        }
                        is CopyingState.DestinationSelected -> {
                            Text(
                                text = buildAnnotatedString {
                                    append("Copying ${copying.settingsDescription()} from ")
                                    withColor(RiftTheme.colors.textHighlighted) {
                                        append(copying.source.name)
                                    }
                                },
                                style = RiftTheme.typography.headerPrimary
                            )

                            val destinationIds = copying.destinations.map { it.character.id }
                            val sourceAccountId = state.characters.firstOrNull { it.characterId == copying.source.id }?.accountId
                            val charactersOnSourceAccount = state.characters.filter { it.accountId == sourceAccountId }.map { it.characterId }
                            val charactersOnTargetAccounts = destinationIds.flatMap { destinationId ->
                                val accountId = state.characters.firstOrNull { it.characterId == destinationId }?.accountId
                                state.characters.filter { it.accountId == accountId }
                            }.distinctBy { it.characterId }
                            val unselectedAffectedCharacters = charactersOnTargetAccounts
                                .filter { it.characterId !in destinationIds }
                                .filter { it.characterId !in charactersOnSourceAccount }
                            val copiesAccountSettings = copying.allSettings ||
                                CopySetting.WindowLayout in copying.settings ||
                                CopySetting.ProbeFormations in copying.settings

                            Text(
                                text = buildAnnotatedString {
                                    append("Pasting to ")
                                    copying.destinations.forEachIndexed { index, character ->
                                        if (index != 0) append(", ")
                                        withColor(RiftTheme.colors.textHighlighted) {
                                            append(character.character.name)
                                        }
                                    }
                                    append(".")
                                },
                                style = RiftTheme.typography.bodyPrimary,
                            )
                            if (copiesAccountSettings && unselectedAffectedCharacters.isNotEmpty()) {
                                Text(
                                    text = buildAnnotatedString {
                                        append("Some settings are account-wide, so these will also affect ")
                                        unselectedAffectedCharacters.map { it.info?.name ?: "${it.characterId}" }.forEachIndexed { index, character ->
                                            if (index != 0) append(", ")
                                            withColor(RiftTheme.colors.textHighlighted) {
                                                append(character)
                                            }
                                        }
                                        append(".")
                                    },
                                    style = RiftTheme.typography.bodyPrimary,
                                )
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        when (val copying = state.copying) {
                            is CopyingState.SelectingSettings -> {
                                RiftButton(
                                    text = "Cancel copying",
                                    type = ButtonType.Secondary,
                                    cornerCut = ButtonCornerCut.None,
                                    onClick = onCancelCopy,
                                )
                                RiftButton(
                                    text = "Confirm settings",
                                    isEnabled = copying.settings.isNotEmpty(),
                                    onClick = onCopySettingsSelected,
                                )
                            }
                            is CopyingState.SelectingDestination -> {
                                RiftButton(
                                    text = "Cancel copying",
                                    type = ButtonType.Secondary,
                                    onClick = onCancelCopy
                                )
                            }
                            is CopyingState.DestinationSelected -> {
                                RiftButton(
                                    text = "Cancel copying",
                                    type = ButtonType.Secondary,
                                    cornerCut = ButtonCornerCut.None,
                                    onClick = onCancelCopy
                                )
                                RiftButton(
                                    text ="Confirm characters",
                                    isEnabled = !state.isOnline,
                                    onClick = onCopyConfirm
                                )
                            }
                        }
                    }

                    Divider(
                        color = RiftTheme.colors.divider,
                        modifier = Modifier.padding(top = Spacing.medium)
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterCopyControls(
    state: UiState,
    character: CharacterItem,
    onCopySource: (Int) -> Unit,
    onCopyDestination: (Int) -> Unit,
    onRevertChanges: (Int) -> Unit,
    onSaveChanges: (Int) -> Unit,
    onBackupSettings: (Int) -> Unit,
    onDeleteBackup: (Int) -> Unit,
) {
    when (val copying = state.copying) {
        CopyingState.SelectingSource -> {
            val hasUnsavedChanges = state.hasChanges(character.settingsId)
            val selectedProfile = state.selectedProfiles[character.settingsId]
            val hasSettingsInSelectedProfile = selectedProfile in character.settingsFiles
            RiftTooltipArea(
                text = when {
                    state.isOnline -> "Close EVE before backing up settings"
                    hasUnsavedChanges -> "Save or revert your changes before backing up settings"
                    else -> "Create a named backup of these settings"
                },
            ) {
                RiftButton(
                    text = "Create backup",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    isEnabled = !state.isOnline && !hasUnsavedChanges && character.accountId != null && hasSettingsInSelectedProfile,
                    onClick = { onBackupSettings(character.settingsId) },
                )
            }
            if (character.backupName != null) {
                RiftButton(
                    text = "Delete backup",
                    type = ButtonType.Negative,
                    cornerCut = ButtonCornerCut.None,
                    onClick = { onDeleteBackup(character.settingsId) },
                )
            }
            RiftTooltipArea(
                text = when {
                    state.isOnline -> "Close EVE before copying settings"
                    hasUnsavedChanges -> "Save or revert your changes before copying settings"
                    else -> "Copy these settings to other characters"
                },
            ) {
                RiftButton(
                    text = "Copy settings",
                    icon = Res.drawable.copy_16px,
                    isEnabled = !state.isOnline && !hasUnsavedChanges && character.accountId != null && hasSettingsInSelectedProfile,
                    onClick = { onCopySource(character.settingsId) }
                )
            }
            AnimatedVisibility(hasUnsavedChanges, enter = fadeIn(), exit = fadeOut()) {
                RiftButton(
                    text = "Revert changes",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = { onRevertChanges(character.settingsId) },
                )
            }
            AnimatedVisibility(hasUnsavedChanges, enter = fadeIn(), exit = fadeOut()) {
                RiftTooltipArea(
                    text = if (state.isOnline) "Close EVE before saving changes" else "Save changes to the EVE settings files",
                ) {
                    RiftButton(
                        text = "Save changes",
                        isEnabled = !state.isOnline,
                        onClick = { onSaveChanges(character.settingsId) },
                    )
                }
            }
        }

        is CopyingState.SelectingDestination -> {
            if (copying.source.id != character.settingsId && character.accountId != null) {
                RiftButton(
                    text = "Paste settings",
                    icon = Res.drawable.recall_drones_16px,
                    isEnabled = !state.isOnline,
                    onClick = { onCopyDestination(character.settingsId) }
                )
            }
        }

        is CopyingState.SelectingSettings -> {}

        is CopyingState.DestinationSelected -> {
            if (copying.source.id != character.settingsId && copying.destinations.none { it.character.id == character.settingsId } && character.accountId != null) {
                RiftButton(
                    text ="Paste settings",
                    icon = Res.drawable.recall_drones_16px,
                    isEnabled = !state.isOnline,
                    onClick = { onCopyDestination(character.settingsId) }
                )
            }
        }
    }
}

private fun CopyingState.SelectingDestination.settingsDescription(): String =
    if (allSettings) "all settings" else settings.joinToString { it.displayName.lowercase() }

private fun CopyingState.DestinationSelected.settingsDescription(): String =
    if (allSettings) "all settings" else settings.joinToString { it.displayName.lowercase() }

@Composable
private fun WindowScope.BackupNameDialog(
    parentWindowState: RiftWindowState,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    RiftDialog(
        title = "Backup settings",
        icon = Res.drawable.window_character_settings,
        parentState = parentWindowState,
        state = rememberWindowState(width = 360.dp, height = Dp.Unspecified),
        onCloseClick = onDismiss,
    ) {
        var name by remember { mutableStateOf("") }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Text("Enter a name for this settings backup", style = RiftTheme.typography.bodyPrimary)
            RiftTextField(
                text = name,
                placeholder = "Backup name",
                onTextChanged = { name = it.take(64) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                RiftButton(
                    text = "Cancel",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.BottomLeft,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                RiftButton(
                    text = "Create backup",
                    isEnabled = name.isNotBlank(),
                    onClick = { onConfirm(name) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WindowScope.DeleteBackupDialog(
    parentWindowState: RiftWindowState,
    backup: CharacterItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    RiftDialog(
        title = "Delete backup",
        icon = Res.drawable.window_character_settings,
        parentState = parentWindowState,
        state = rememberWindowState(width = 520.dp, height = Dp.Unspecified),
        onCloseClick = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
            Text(
                text = "Delete ${backup.info?.name ?: backup.characterId} - ${backup.backupName}?",
                style = RiftTheme.typography.headerPrimary,
            )
            Text(
                text = "The following files will be permanently deleted:",
                style = RiftTheme.typography.bodySecondary,
            )
            (backup.settingsFiles.values + backup.accountSettingsFiles.values).forEach { path ->
                Text(
                    text = path.toString(),
                    style = RiftTheme.typography.detailPrimary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                RiftButton(
                    text = "Cancel",
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.BottomLeft,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                RiftButton(
                    text = "Delete backup",
                    type = ButtonType.Negative,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun EmptyCharacterPage() {
    Text(
        text = "No characters found.\n\nMake sure the game directory is selected in settings, and that you have logged in to at least one character on this computer before.",
        style = RiftTheme.typography.headerPrimary,
        modifier = Modifier.fillMaxSize().padding(Spacing.large)
    )
}
