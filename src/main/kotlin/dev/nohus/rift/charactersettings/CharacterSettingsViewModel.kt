package dev.nohus.rift.charactersettings

import dev.nohus.rift.ViewModel
import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.OnlineCharactersRepository
import dev.nohus.rift.charactersettings.compose.NeocomButtonDefinitionsById
import dev.nohus.rift.charactersettings.io.AccountAssociationsRepository
import dev.nohus.rift.charactersettings.io.CopyEveCharacterSettingsUseCase
import dev.nohus.rift.charactersettings.io.GetAccountsUseCase
import dev.nohus.rift.compose.DialogMessage
import dev.nohus.rift.compose.MessageDialogType
import dev.nohus.rift.repositories.character.CharacterDetailsRepository.CharacterDetails
import dev.nohus.rift.repositories.character.CharactersRepository
import dev.nohus.rift.network.requests.Originator
import androidx.compose.ui.graphics.Color
import dev.nohus.rift.charactersettings.io.GetEveSettingsBackupsUseCase
import dev.nohus.rift.charactersettings.io.ReadAccountSettingsUseCase
import dev.nohus.rift.charactersettings.io.ReadCharacterSettingsUseCase
import dev.nohus.rift.charactersettings.io.WriteEveSettingsUseCase
import dev.nohus.rift.charactersettings.io.WriteEveSettingsUseCase.AccountSection
import dev.nohus.rift.charactersettings.io.WriteEveSettingsUseCase.CharacterSection
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.directories.IsEveSharedCacheDirectoryValidUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.createDirectories
import kotlin.io.path.nameWithoutExtension
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Factory
class CharacterSettingsViewModel(
    private val copyEveCharacterSettingsUseCase: CopyEveCharacterSettingsUseCase,
    private val localCharactersRepository: LocalCharactersRepository,
    private val onlineCharactersRepository: OnlineCharactersRepository,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val readCharacterSettingsUseCase: ReadCharacterSettingsUseCase,
    private val readAccountSettingsUseCase: ReadAccountSettingsUseCase,
    private val writeEveSettingsUseCase: WriteEveSettingsUseCase,
    private val accountAssociationsRepository: AccountAssociationsRepository,
    private val charactersRepository: CharactersRepository,
    private val settings: Settings,
    private val getEveSettingsBackupsUseCase: GetEveSettingsBackupsUseCase,
    private val categorizeWindowUseCase: CategorizeWindowUseCase,
    private val isEveSharedCacheDirectoryValidUseCase: IsEveSharedCacheDirectoryValidUseCase,
) : ViewModel() {

    data class CharacterItem(
        val characterId: Int,
        val accountId: Int?,
        val settingsFiles: Map<String, Path>,
        val info: CharacterDetails?,
        val settingsId: Int = characterId,
        val backupName: String? = null,
        val backupTimestamp: Instant? = null,
        val accountSettingsFiles: Map<String, Path> = emptyMap(),
    )

    data class CharacterSettings(
        val characterSettings: ReadCharacterSettingsUseCase.CharacterSettings?,
        val accountSettings: ReadAccountSettingsUseCase.AccountSettings?,
    )

    data class UiState(
        val characters: List<CharacterItem> = emptyList(),
        val accounts: List<GetAccountsUseCase.Account> = emptyList(),
        val profiles: List<String> = emptyList(),
        val selectedCharacterId: Int? = null,
        val selectedProfiles: Map<Int, String> = emptyMap(),
        val settings: Map<Int, CharacterSettings> = emptyMap(),
        val changedSettings: Map<Int, CharacterSettings> = emptyMap(),
        val copying: CopyingState = CopyingState.SelectingSource,
        val isOnline: Boolean,
        val dialogMessage: DialogMessage? = null,
        val watchlistCharacterId: Int? = null,
        val watchlistCharacterName: String = "",
        val isWatchlistCharacterLookupComplete: Boolean = false,
        val backupDialogCharacterId: Int? = null,
        val deleteBackupDialogCharacterId: Int? = null,
        val isSharedCacheMissing: Boolean = false,
    ) {
        val displayedSettings: Map<Int, CharacterSettings> get() = settings + changedSettings
        fun hasChanges(characterId: Int): Boolean = characterId in changedSettings
    }

    sealed interface CopyingState {
        data object SelectingSource : CopyingState

        data class SelectingSettings(
            val source: CopyingCharacter,
            val sourceProfile: String,
            val settings: Set<CopySetting> = emptySet(),
            val allSettings: Boolean = false,
        ) : CopyingState

        data class SelectingDestination(
            val source: CopyingCharacter,
            val sourceProfile: String,
            val settings: Set<CopySetting>,
            val allSettings: Boolean,
        ) : CopyingState

        data class DestinationSelected(
            val source: CopyingCharacter,
            val destinations: List<CopyingDestination>,
            val sourceProfile: String,
            val settings: Set<CopySetting>,
            val allSettings: Boolean,
        ) : CopyingState
    }

    enum class CopySetting(val displayName: String) {
        WindowLayout("Window layout"),
        NeocomButtons("Neocom buttons"),
        FleetWatchlist("Fleet watchlist"),
        ChatChannels("Chat channels"),
        ProbeFormations("Probe formations"),
    }

    data class CopyingCharacter(val character: CharacterItem) {
        val id get() = character.settingsId
        val name get() = character.backupName?.let { backupName ->
            "${character.info?.name ?: character.characterId} - $backupName"
        } ?: character.info?.name ?: character.characterId.toString()
    }

    data class CopyingDestination(
        val character: CopyingCharacter,
        val profile: String
    )

    private val _state = MutableStateFlow(
        UiState(
            isOnline = onlineCharactersRepository.onlineCharacters.value.isNotEmpty()
        )
    )
    val state = _state.asStateFlow()

    private val selectedProfiles = MutableStateFlow<Map<Int, String>>(emptyMap())
    private val backupRefresh = MutableStateFlow(0)
    private var loadedFileTimestamps = emptyMap<Path, Long>()

    init {
        viewModelScope.launch {
            localCharactersRepository.load()
        }
        viewModelScope.launch {
            combine(
                localCharactersRepository.characters,
                settings.updateFlow,
                selectedProfiles,
                backupRefresh,
            ) { localCharacters, _, selectedProfiles, _ ->
                delay(500.milliseconds) // Allow account associations to settle
                val associations = accountAssociationsRepository.getAssociations()
                val regularCharacters = localCharacters.map {
                    CharacterItem(
                        characterId = it.characterId,
                        accountId = associations[it.characterId],
                        settingsFiles = it.settingsFiles,
                        info = it.info
                    )
                }
                val accounts = getAccountsUseCase()
                val backups = getEveSettingsBackupsUseCase().map { backup ->
                    CharacterItem(
                        characterId = backup.characterId,
                        accountId = backup.accountId,
                        settingsFiles = mapOf(backup.profile to backup.characterFile),
                        info = regularCharacters.firstOrNull { it.characterId == backup.characterId }?.info,
                        settingsId = backup.id,
                        backupName = backup.name,
                        backupTimestamp = backup.timestamp,
                        accountSettingsFiles = mapOf(backup.profile to backup.accountFile),
                    )
                }
                val characters = regularCharacters + backups
                val allProfiles = (regularCharacters.flatMap { it.settingsFiles.keys } + accounts.flatMap { it.paths.keys })
                    .distinct()
                    .sorted()
                val profiles = characters.mapNotNull { character ->
                    val profile = character.settingsFiles.keys.firstOrNull()
                        ?: allProfiles.firstOrNull().takeIf { character.backupName == null }
                        ?: return@mapNotNull null
                    character.settingsId to profile
                }.toMap() + selectedProfiles
                _state.update { old ->
                    old.copy(
                        characters = characters,
                        accounts = accounts,
                        profiles = allProfiles,
                        selectedCharacterId = old.selectedCharacterId ?: regularCharacters.firstOrNull()?.settingsId,
                        selectedProfiles = profiles,
                        settings = characters.associate { character ->
                            val characterSettingsFile = character.settingsFiles[profiles[character.settingsId]]
                            val accountSettingsFile = getAccountSettingsFile(character, profiles[character.settingsId], accounts)
                            val characterSettings = CharacterSettings(
                                characterSettings = readCharacterSettingsUseCase(characterSettingsFile),
                                accountSettings = readAccountSettingsUseCase(accountSettingsFile)
                            )
                            character.settingsId to characterSettings
                        },
                        isSharedCacheMissing = !isEveSharedCacheDirectoryValidUseCase(settings.eveSharedCacheDirectory),
                    )
                }
            }.collect()
        }
        viewModelScope.launch {
            onlineCharactersRepository.onlineCharacters.collect { onlineCharacters ->
                _state.update { it.copy(isOnline = onlineCharacters.isNotEmpty()) }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(10.seconds)
                reloadExternallyChangedSettings()
                backupRefresh.update { it + 1 }
            }
        }
    }

    private fun reloadExternallyChangedSettings() {
        val state = state.value
        val loadedFiles = state.characters.flatMap { character ->
            val profile = state.selectedProfiles[character.settingsId] ?: return@flatMap emptyList()
            val characterFile = character.settingsFiles[profile]?.let {
                LoadedSettingsFile(character.settingsId, it, isAccountSettings = false)
            }
            val accountFile = getAccountSettingsFile(character, profile, state.accounts)
                ?.let { LoadedSettingsFile(character.settingsId, it, isAccountSettings = true) }
            listOfNotNull(characterFile, accountFile)
        }
        val currentTimestamps = loadedFiles
            .associate { it.path to runCatching { it.path.getLastModifiedTime().toMillis() }.getOrDefault(0L) }
        if (loadedFileTimestamps.isEmpty()) {
            loadedFileTimestamps = currentTimestamps
            return
        }
        val changedFiles = loadedFiles.filter { file ->
            val previousTimestamp = loadedFileTimestamps[file.path]
            previousTimestamp != null && previousTimestamp != currentTimestamps[file.path]
        }
        loadedFileTimestamps = currentTimestamps
        if (changedFiles.isEmpty()) return

        _state.update { currentState ->
            val updatedSettings = currentState.settings.toMutableMap()
            changedFiles.forEach { file ->
                val original = updatedSettings[file.characterId] ?: return@forEach
                updatedSettings[file.characterId] = if (file.isAccountSettings) {
                    val reloaded = runCatching { readAccountSettingsUseCase(file.path) }.getOrNull()
                        ?: return@forEach
                    original.copy(accountSettings = reloaded)
                } else {
                    val reloaded = runCatching { readCharacterSettingsUseCase(file.path) }.getOrNull()
                        ?: return@forEach
                    original.copy(characterSettings = reloaded)
                }
            }
            currentState.copy(settings = updatedSettings)
        }
    }

    private data class LoadedSettingsFile(
        val characterId: Int,
        val path: Path,
        val isAccountSettings: Boolean,
    )

    fun onCharacterSelected(characterId: Int) {
        _state.update { it.copy(selectedCharacterId = characterId) }
    }

    fun onProfileSelected(characterId: Int, profile: String) {
        _state.update {
            it.copy(
                changedSettings = it.changedSettings - characterId,
            )
        }
        selectedProfiles.update { it + (characterId to profile) }
    }

    fun onAssignAccount(characterId: Int, accountId: Int) {
        accountAssociationsRepository.associate(characterId, accountId)
    }

    fun onCopySourceClick(characterId: Int) {
        if (state.value.isOnline) return
        val character = state.value.characters.firstOrNull { it.settingsId == characterId } ?: return
        val profile = state.value.selectedProfiles[characterId] ?: character.settingsFiles.keys.firstOrNull() ?: return
        if (profile !in character.settingsFiles) return
        _state.update { it.copy(copying = CopyingState.SelectingSettings(CopyingCharacter(character), profile)) }
    }

    fun onCopySettingChanged(setting: CopySetting?, isSelected: Boolean) {
        val copying = state.value.copying as? CopyingState.SelectingSettings ?: return
        val updated = if (setting == null) {
            copying.copy(
                settings = if (isSelected) CopySetting.entries.toSet() else emptySet(),
                allSettings = isSelected,
            )
        } else {
            copying.copy(
                settings = if (isSelected) copying.settings + setting else copying.settings - setting,
                allSettings = false,
            )
        }
        _state.update { it.copy(copying = updated) }
    }

    fun onCopySettingsSelected() {
        val copying = state.value.copying as? CopyingState.SelectingSettings ?: return
        if (copying.settings.isEmpty()) return
        _state.update {
            it.copy(copying = CopyingState.SelectingDestination(
                source = copying.source,
                sourceProfile = copying.sourceProfile,
                settings = copying.settings,
                allSettings = copying.allSettings,
            ))
        }
    }

    fun onCopyDestinationClick(characterId: Int) {
        if (state.value.isOnline) return
        val destination = state.value.characters
            .firstOrNull { it.settingsId == characterId }
            ?.let(::CopyingCharacter) ?: return
        val profile = state.value.selectedProfiles[characterId]
            ?: state.value.profiles.firstOrNull()
            ?: return
        val selectedDestination = CopyingDestination(destination, profile)

        when (val copying = state.value.copying) {
            is CopyingState.SelectingDestination -> {
                _state.update {
                    it.copy(
                        copying = CopyingState.DestinationSelected(
                            source = copying.source,
                            destinations = listOf(selectedDestination),
                            sourceProfile = copying.sourceProfile,
                            settings = copying.settings,
                            allSettings = copying.allSettings,
                        )
                    )
                }
            }

            is CopyingState.DestinationSelected -> {
                if (copying.destinations.none { it.character.id == destination.id }) {
                    _state.update {
                        it.copy(
                            copying = copying.copy(destinations = copying.destinations + selectedDestination)
                        )
                    }
                }
            }

            else -> {}
        }
    }

    fun onCopySettingsConfirmClick() {
        if (state.value.isOnline) return
        val copying = state.value.copying as? CopyingState.DestinationSelected ?: return
        copy(copying.source, copying.destinations, copying.sourceProfile, copying.settings, copying.allSettings)
    }

    private fun copy(
        source: CopyingCharacter,
        destinations: List<CopyingDestination>,
        sourceProfile: String,
        selectedSettings: Set<CopySetting>,
        allSettings: Boolean,
    ) {
        val sourceCharacterFile = source.character.settingsFiles[sourceProfile]
        val sourceAccountFile = getAccountSettingsFile(source.character, sourceProfile, state.value.accounts)
        val success = sourceCharacterFile != null && sourceAccountFile != null && destinations.all { destination ->
            runCatching {
                val target = destination.character.character
                val targetCharacterFile = target.settingsFiles[destination.profile].takeIf { target.backupName != null }
                    ?: sourceCharacterFile.parent.parent.resolve("settings_${destination.profile}")
                        .resolve("core_char_${target.characterId}.dat")
                val targetAccountFile = target.accountSettingsFiles[destination.profile].takeIf { target.backupName != null }
                    ?: target.accountId?.let { accountId ->
                        sourceCharacterFile.parent.parent.resolve("settings_${destination.profile}")
                            .resolve("core_user_$accountId.dat")
                    }
                targetAccountFile != null && if (allSettings) {
                    copyEveCharacterSettingsUseCase.copyFiles(
                        fromCharacterFile = sourceCharacterFile,
                        fromAccountFile = sourceAccountFile,
                        toCharacterFiles = listOf(targetCharacterFile),
                        toAccountFiles = listOf(targetAccountFile),
                    )
                } else {
                    if (!targetCharacterFile.exists()) {
                        targetCharacterFile.parent.createDirectories()
                        sourceCharacterFile.copyTo(targetCharacterFile)
                    }
                    if (!targetAccountFile.exists()) {
                        targetAccountFile.parent.createDirectories()
                        sourceAccountFile.copyTo(targetAccountFile)
                    }
                    copyPartialSettings(sourceCharacterFile, sourceAccountFile, targetCharacterFile, targetAccountFile, selectedSettings)
                }
            }.getOrDefault(false)
        }
        _state.update {
            it.copy(
                dialogMessage = DialogMessage(
                    title = if (success) "Settings copied" else "Copying failed",
                    message = if (success) "EVE settings have been copied from ${source.name} to ${destinations.joinToString { it.character.name }}." else "There is something wrong with your character settings files.",
                    type = if (success) MessageDialogType.Info else MessageDialogType.Warning
                )
            )
        }
        if (success) {
            backupRefresh.update { it + 1 }
            viewModelScope.launch { localCharactersRepository.load() }
        }
    }

    private fun copyPartialSettings(
        sourceCharacterFile: Path,
        sourceAccountFile: Path,
        targetCharacterFile: Path,
        targetAccountFile: Path,
        selectedSettings: Set<CopySetting>,
    ): Boolean {
        val sourceCharacter = readCharacterSettingsUseCase(sourceCharacterFile) ?: return false
        val targetCharacter = readCharacterSettingsUseCase(targetCharacterFile) ?: return false
        val sourceAccount = readAccountSettingsUseCase(sourceAccountFile) ?: return false
        val targetAccount = readAccountSettingsUseCase(targetAccountFile) ?: return false

        val characterSettings = targetCharacter.copy(
            openWindows = sourceCharacter.openWindows.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetCharacter.openWindows,
            minimizedWindows = sourceCharacter.minimizedWindows.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetCharacter.minimizedWindows,
            windowStacks = sourceCharacter.windowStacks.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetCharacter.windowStacks,
            windowSizesAndPositions = sourceCharacter.windowSizesAndPositions.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetCharacter.windowSizesAndPositions,
            screenResolution = sourceCharacter.screenResolution.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetCharacter.screenResolution,
            uiScale = if (CopySetting.WindowLayout in selectedSettings) sourceCharacter.uiScale else targetCharacter.uiScale,
            neocomWidthPx = sourceCharacter.neocomWidthPx.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetCharacter.neocomWidthPx,
            shipUiLeftOffsetPx = sourceCharacter.shipUiLeftOffsetPx.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetCharacter.shipUiLeftOffsetPx,
            rawOpenWindows = sourceCharacter.rawOpenWindows.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetCharacter.rawOpenWindows,
            rawMinimizedWindows = sourceCharacter.rawMinimizedWindows.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetCharacter.rawMinimizedWindows,
            rawWindowStacks = sourceCharacter.rawWindowStacks.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetCharacter.rawWindowStacks,
            rawWindowSizesAndPositions = sourceCharacter.rawWindowSizesAndPositions.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetCharacter.rawWindowSizesAndPositions,
            neocomIconColors = sourceCharacter.neocomIconColors.takeIf { CopySetting.NeocomButtons in selectedSettings } ?: targetCharacter.neocomIconColors,
            neocomButtons = sourceCharacter.neocomButtons.takeIf { CopySetting.NeocomButtons in selectedSettings } ?: targetCharacter.neocomButtons,
            rawNeocomButtons = sourceCharacter.rawNeocomButtons.takeIf { CopySetting.NeocomButtons in selectedSettings } ?: targetCharacter.rawNeocomButtons,
            rawNeocomIconColors = sourceCharacter.rawNeocomIconColors.takeIf { CopySetting.NeocomButtons in selectedSettings } ?: targetCharacter.rawNeocomIconColors,
            watchlistColors = sourceCharacter.watchlistColors.takeIf { CopySetting.FleetWatchlist in selectedSettings } ?: targetCharacter.watchlistColors,
            rawWatchlistColors = sourceCharacter.rawWatchlistColors.takeIf { CopySetting.FleetWatchlist in selectedSettings } ?: targetCharacter.rawWatchlistColors,
            parsedWatchlistColorKeys = sourceCharacter.parsedWatchlistColorKeys.takeIf { CopySetting.FleetWatchlist in selectedSettings }
                ?: targetCharacter.parsedWatchlistColorKeys,
            joinedChatChannels = sourceCharacter.joinedChatChannels.takeIf { CopySetting.ChatChannels in selectedSettings } ?: targetCharacter.joinedChatChannels,
            rawJoinedChatChannels = sourceCharacter.rawJoinedChatChannels.takeIf { CopySetting.ChatChannels in selectedSettings } ?: targetCharacter.rawJoinedChatChannels,
            parsedChatChannelIds = sourceCharacter.parsedChatChannelIds.takeIf { CopySetting.ChatChannels in selectedSettings }
                ?: targetCharacter.parsedChatChannelIds,
        )
        val accountSettings = targetAccount.copy(
            isShipUiOnTop = sourceAccount.isShipUiOnTop.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetAccount.isShipUiOnTop,
            targetOrigin = if (CopySetting.WindowLayout in selectedSettings) sourceAccount.targetOrigin else targetAccount.targetOrigin,
            isTargetsAlignHorizontal = sourceAccount.isTargetsAlignHorizontal.takeIf { CopySetting.WindowLayout in selectedSettings } ?: targetAccount.isTargetsAlignHorizontal,
            probeFormations = sourceAccount.probeFormations.takeIf { CopySetting.ProbeFormations in selectedSettings } ?: targetAccount.probeFormations,
            rawProbeFormations = sourceAccount.rawProbeFormations.takeIf { CopySetting.ProbeFormations in selectedSettings } ?: targetAccount.rawProbeFormations,
            parsedProbeFormationKeys = sourceAccount.parsedProbeFormationKeys.takeIf { CopySetting.ProbeFormations in selectedSettings }
                ?: targetAccount.parsedProbeFormationKeys,
        )
        val characterSections = selectedSettings.mapNotNullTo(mutableSetOf()) {
            when (it) {
                CopySetting.WindowLayout -> CharacterSection.WindowLayout
                CopySetting.NeocomButtons -> CharacterSection.NeocomButtons
                CopySetting.FleetWatchlist -> CharacterSection.FleetWatchlist
                CopySetting.ChatChannels -> CharacterSection.ChatChannels
                CopySetting.ProbeFormations -> null
            }
        }
        val accountSections = selectedSettings.mapNotNullTo(mutableSetOf()) {
            when (it) {
                CopySetting.WindowLayout -> AccountSection.WindowLayout
                CopySetting.ProbeFormations -> AccountSection.ProbeFormations
                else -> null
            }
        }
        val characterWritten = characterSections.isEmpty() ||
            writeEveSettingsUseCase.writeCharacterSettings(targetCharacterFile, characterSettings, characterSections)
        return characterWritten && (accountSections.isEmpty() ||
            writeEveSettingsUseCase.writeAccountSettings(targetAccountFile, accountSettings, accountSections))
    }

    fun onCloseDialogMessage() {
        _state.update { it.copy(dialogMessage = null) }
        onCancelCopy()
    }

    fun onCancelCopy() {
        _state.update { it.copy(copying = CopyingState.SelectingSource) }
    }

    fun onBackupSettingsClick(characterId: Int) {
        if (state.value.isOnline || state.value.hasChanges(characterId)) return
        _state.update { it.copy(backupDialogCharacterId = characterId) }
    }

    fun onBackupDialogDismissed() {
        _state.update { it.copy(backupDialogCharacterId = null) }
    }

    fun onDeleteBackupClick(characterId: Int) {
        val character = state.value.characters.firstOrNull { it.settingsId == characterId } ?: return
        if (character.backupName == null) return
        _state.update { it.copy(deleteBackupDialogCharacterId = characterId) }
    }

    fun onDeleteBackupDialogDismissed() {
        _state.update { it.copy(deleteBackupDialogCharacterId = null) }
    }

    fun onDeleteBackupConfirm() {
        val current = state.value
        val settingsId = current.deleteBackupDialogCharacterId ?: return
        val backup = current.characters.firstOrNull { it.settingsId == settingsId && it.backupName != null } ?: return
        val success = runCatching {
            backup.settingsFiles.values.forEach { it.deleteIfExists() }
            backup.accountSettingsFiles.values.forEach { it.deleteIfExists() }
        }.isSuccess
        if (success) selectedProfiles.update { it - settingsId }
        _state.update {
            it.copy(
                selectedCharacterId = if (success) {
                    it.characters.firstOrNull { character -> character.backupName == null }
                        ?.settingsId
                } else {
                    it.selectedCharacterId
                },
                changedSettings = if (success) it.changedSettings - settingsId else it.changedSettings,
                deleteBackupDialogCharacterId = null,
                dialogMessage = DialogMessage(
                    title = if (success) "Backup deleted" else "Deleting failed",
                    message = if (success) "The settings backup has been deleted" else "The backup files could not be deleted",
                    type = if (success) MessageDialogType.Info else MessageDialogType.Warning,
                ),
            )
        }
        if (success) backupRefresh.update { it + 1 }
    }

    fun onCreateBackup(name: String) {
        val current = state.value
        val settingsId = current.backupDialogCharacterId ?: return
        val character = current.characters.firstOrNull { it.settingsId == settingsId } ?: return
        val profile = current.selectedProfiles[settingsId] ?: return
        val characterFile = character.settingsFiles[profile] ?: return
        val accountFile = getAccountSettingsFile(character, profile, current.accounts) ?: return
        val safeName = name.trim().replace(Regex("[^A-Za-z0-9-]+"), "_").trim('_')
        if (safeName.isEmpty()) return
        val success = runCatching {
            val characterBaseName = characterFile.nameWithoutExtension.substringBefore("_rift_backup_")
            val accountBaseName = accountFile.nameWithoutExtension.substringBefore("_rift_backup_")
            var suffix = safeName
            var count = 2
            var characterBackup = characterFile.parent.resolve("${characterBaseName}_rift_backup_$suffix.dat")
            var accountBackup = accountFile.parent.resolve("${accountBaseName}_rift_backup_$suffix.dat")
            while (characterBackup.exists() || accountBackup.exists()) {
                suffix = "${safeName}_${count++}"
                characterBackup = characterFile.parent.resolve("${characterBaseName}_rift_backup_$suffix.dat")
                accountBackup = accountFile.parent.resolve("${accountBaseName}_rift_backup_$suffix.dat")
            }
            characterFile.copyTo(characterBackup)
            accountFile.copyTo(accountBackup)
        }.isSuccess
        _state.update {
            it.copy(
                backupDialogCharacterId = null,
                dialogMessage = DialogMessage(
                    title = if (success) "Backup created" else "Backup failed",
                    message = if (success) "The settings backup has been created" else "There is something wrong with your character or account settings file",
                    type = if (success) MessageDialogType.Info else MessageDialogType.Warning,
                ),
            )
        }
        if (success) backupRefresh.update { it + 1 }
    }

    fun onWatchlistCharacterNameChanged(name: String) {
        _state.update {
            it.copy(
                watchlistCharacterName = name,
                watchlistCharacterId = null,
                isWatchlistCharacterLookupComplete = false,
            )
        }
        if (name.isBlank()) return
        viewModelScope.launch {
            delay(200.milliseconds)
            val characterId = charactersRepository.getCharacterId(Originator.CharacterSettingsManagement, name)
            // Ignore a completed lookup when the user has moved on to another name
            if (state.value.watchlistCharacterName == name) {
                _state.update { it.copy(watchlistCharacterId = characterId, isWatchlistCharacterLookupComplete = true) }
            }
        }
    }

    fun onWatchlistUpdated(characterId: Int, color: Color?) {
        val selectedCharacterId = state.value.selectedCharacterId ?: return
        val watchlist = state.value.displayedSettings[selectedCharacterId]?.characterSettings?.watchlistColors ?: return
        val updatedWatchlist = watchlist + (characterId to color)
        updateWatchlist(selectedCharacterId, updatedWatchlist)
    }

    fun onWatchlistCharacterRemoved(characterId: Int) {
        val selectedCharacterId = state.value.selectedCharacterId ?: return
        val watchlist = state.value.displayedSettings[selectedCharacterId]?.characterSettings?.watchlistColors ?: return
        val updatedWatchlist = watchlist - characterId
        updateWatchlist(selectedCharacterId, updatedWatchlist)
    }

    fun onNeocomButtonColorChanged(buttonId: String, colorId: Int?) {
        val characterId = state.value.selectedCharacterId ?: return
        updateNeocomButtons(characterId) { buttons ->
            buttons.map { button ->
                if (button.id == buttonId) {
                    button.copy(colorId = colorId)
                } else {
                    button.copy(children = updateButtonColor(button.children, buttonId, colorId))
                }
            }
        }
    }

    fun onNeocomButtonsReordered(parentId: String?, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val characterId = state.value.selectedCharacterId ?: return
        updateNeocomButtons(characterId) { buttons ->
            if (parentId == null) {
                buttons.move(fromIndex, toIndex)
            } else {
                buttons.map { button ->
                    if (button.id == parentId) {
                        button.copy(children = button.children.move(fromIndex, toIndex))
                    } else {
                        button.copy(children = reorderChildren(button.children, parentId, fromIndex, toIndex))
                    }
                }
            }
        }
    }

    fun onNeocomButtonAdded(buttonId: String) {
        val definition = NeocomButtonDefinitionsById[buttonId]?.takeIf { it.isAddable } ?: return
        val characterId = state.value.selectedCharacterId ?: return
        updateNeocomButtons(characterId) { buttons ->
            if (buttons.containsNeocomButton(buttonId)) return@updateNeocomButtons buttons
            buttons + ReadCharacterSettingsUseCase.NeocomButton(
                btnType = definition.btnType,
                id = definition.id,
                label = definition.name,
                iconPath = definition.iconPath,
                colorId = null,
                children = emptyList(),
            )
        }
    }

    fun onNeocomButtonRemoved(buttonId: String) {
        val characterId = state.value.selectedCharacterId ?: return
        updateNeocomButtons(characterId) { buttons -> buttons.removeNeocomButton(buttonId) }
    }

    fun onNeocomWidthChanged(width: Int) = updateSelectedCharacterSettings {
        it.copy(neocomWidthPx = width)
    }

    fun onWindowBoundsChanged(
        window: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        screenResolution: Pair<Int, Int>,
    ) {
        val characterId = state.value.selectedCharacterId ?: return
        _state.update { state ->
            val currentSettings = state.displayedSettings[characterId] ?: return@update state
            val characterSettings = currentSettings.characterSettings ?: return@update state
            val position = characterSettings.windowSizesAndPositions[window] ?: return@update state
            if (position.size < 6) return@update state

            val updatedPositions = characterSettings.windowSizesAndPositions + (window to position.toMutableList().also {
                it[0] = x
                it[1] = y
                it[2] = width
                it[3] = height
                it[4] = screenResolution.first
                it[5] = screenResolution.second
            })
            state.copy(changedSettings = state.changedSettings + (characterId to currentSettings.copy(
                characterSettings = characterSettings.copy(windowSizesAndPositions = updatedPositions),
            )))
        }
    }

    fun onWindowMinimizedChanged(window: String) {
        val characterId = state.value.selectedCharacterId ?: return
        _state.update { state ->
            val currentSettings = state.displayedSettings[characterId] ?: return@update state
            val characterSettings = currentSettings.characterSettings ?: return@update state
            if (window !in characterSettings.windowSizesAndPositions) return@update state
            val isMinimized = window in characterSettings.minimizedWindows
            if (!isMinimized) {
                val childrenWindows = characterSettings.windowStacks.filter { it.second == window }.map { it.first }
                val eveWindow = categorizeWindowUseCase(window, childrenWindows, characterSettings.joinedChatChannels)
                if (!eveWindow.isMinimizedStatePersistent) {
                    return@update state.copy(
                        dialogMessage = DialogMessage(
                            title = "Minimized windows",
                            message = "EVE only remembers the minimized state of chat windows, so minimizing this window will have no in-game effect.",
                            type = MessageDialogType.Info,
                        )
                    )
                }
            }
            val minimizedWindows = if (isMinimized) {
                characterSettings.minimizedWindows - window
            } else {
                characterSettings.minimizedWindows + window
            }
            state.copy(changedSettings = state.changedSettings + (characterId to currentSettings.copy(
                characterSettings = characterSettings.copy(minimizedWindows = minimizedWindows),
            )))
        }
    }

    fun onWindowOpened(window: String, screenResolution: Pair<Int, Int>) {
        val characterId = state.value.selectedCharacterId ?: return
        _state.update { state ->
            val currentSettings = state.displayedSettings[characterId] ?: return@update state
            val characterSettings = currentSettings.characterSettings ?: return@update state
            val defaultBounds = listOf(
                screenResolution.first / 4,
                screenResolution.second / 4,
                screenResolution.first / 3,
                screenResolution.second / 3,
                screenResolution.first,
                screenResolution.second,
            )
            state.copy(changedSettings = state.changedSettings + (characterId to currentSettings.copy(
                characterSettings = characterSettings.copy(
                    openWindows = (characterSettings.openWindows + window).distinct(),
                    minimizedWindows = characterSettings.minimizedWindows - window,
                    windowSizesAndPositions = characterSettings.windowSizesAndPositions +
                        (window to (characterSettings.windowSizesAndPositions[window] ?: defaultBounds)),
                ),
            )))
        }
    }

    fun onWindowClosed(window: String) {
        val characterId = state.value.selectedCharacterId ?: return
        _state.update { state ->
            val currentSettings = state.displayedSettings[characterId] ?: return@update state
            val characterSettings = currentSettings.characterSettings ?: return@update state
            val childrenWindows = characterSettings.windowStacks.filter { it.second == window }.map { it.first }
            val eveWindow = categorizeWindowUseCase(window, childrenWindows, characterSettings.joinedChatChannels)
            if (!eveWindow.isCloseable) return@update state
            val windowsToClose = (childrenWindows + window).toSet()
            val joinedChatChannelsToClose = windowsToClose.mapNotNull { windowId ->
                windowId
                    .takeIf { it.startsWith("utf8:chatchannel_") }
                    ?.substringAfter("channel_")
                    ?.takeIf { it in characterSettings.joinedChatChannels }
            }.toSet()
            state.copy(changedSettings = state.changedSettings + (characterId to currentSettings.copy(
                characterSettings = characterSettings.copy(
                    openWindows = characterSettings.openWindows - windowsToClose,
                    minimizedWindows = characterSettings.minimizedWindows - windowsToClose,
                    joinedChatChannels = characterSettings.joinedChatChannels - joinedChatChannelsToClose,
                ),
            )))
        }
    }

    fun onShipUiOffsetChanged(offset: Float) = updateSelectedCharacterSettings {
        it.copy(shipUiLeftOffsetPx = offset)
    }

    fun onShipUiVerticalPositionChanged() = updateSelectedAccountSettings {
        it.copy(isShipUiOnTop = !it.isShipUiOnTop)
    }

    fun onTargetOriginChanged(origin: Pair<Float, Float>) = updateSelectedAccountSettings {
        it.copy(targetOrigin = origin)
    }

    fun onTargetsAlignmentChanged() = updateSelectedAccountSettings {
        it.copy(isTargetsAlignHorizontal = !it.isTargetsAlignHorizontal)
    }

    fun onChatChannelsChanged(channels: Map<String, String>) = updateSelectedCharacterSettings {
        it.copy(joinedChatChannels = channels)
    }

    fun onProbeFormationsChanged(formations: List<ReadAccountSettingsUseCase.ProbeFormation>) = updateSelectedAccountSettings {
        it.copy(probeFormations = formations)
    }

    fun onRevertChanges(characterId: Int) {
        _state.update {
            it.copy(
                changedSettings = it.changedSettings - characterId,
            )
        }
    }

    fun onSaveChanges(characterId: Int) {
        val state = state.value
        if (state.isOnline) return
        val changed = state.changedSettings[characterId] ?: return
        val original = state.settings[characterId] ?: return
        val character = state.characters.firstOrNull { it.settingsId == characterId } ?: return
        val profile = state.selectedProfiles[characterId] ?: return
        val characterPath = character.settingsFiles[profile]
        val accountPath = getAccountSettingsFile(character, profile, state.accounts)
        val characterSuccess = if (changed.characterSettings != original.characterSettings) {
            characterPath != null && changed.characterSettings != null &&
                writeEveSettingsUseCase.writeCharacterSettings(
                    characterPath,
                    changed.characterSettings,
                    getChangedCharacterSections(original.characterSettings, changed.characterSettings),
                )
        } else {
            true
        }
        val accountSuccess = if (changed.accountSettings != original.accountSettings) {
            accountPath != null && changed.accountSettings != null &&
                writeEveSettingsUseCase.writeAccountSettings(
                    accountPath,
                    changed.accountSettings,
                    getChangedAccountSections(original.accountSettings, changed.accountSettings),
                )
        } else {
            true
        }
        val success = characterSuccess && accountSuccess
        _state.update {
            it.copy(
                settings = if (success) it.settings + (characterId to changed) else it.settings,
                changedSettings = if (success) it.changedSettings - characterId else it.changedSettings,
                dialogMessage = DialogMessage(
                    title = if (success) "Settings saved" else "Saving failed",
                    message = if (success) {
                        "Your EVE settings changes have been saved"
                    } else {
                        "There is something wrong with your character or account settings file"
                    },
                    type = if (success) MessageDialogType.Info else MessageDialogType.Warning,
                ),
            )
        }
        if (success) backupRefresh.update { it + 1 }
    }

    private fun updateSelectedCharacterSettings(update: (ReadCharacterSettingsUseCase.CharacterSettings) -> ReadCharacterSettingsUseCase.CharacterSettings) {
        val characterId = state.value.selectedCharacterId ?: return
        _state.update { state ->
            val settings = state.displayedSettings[characterId] ?: return@update state
            val characterSettings = settings.characterSettings ?: return@update state
            val updatedCharacterSettings = update(characterSettings)
            if (updatedCharacterSettings == characterSettings) return@update state
            state.copy(changedSettings = state.changedSettings + (characterId to settings.copy(characterSettings = updatedCharacterSettings)))
        }
    }

    private fun updateSelectedAccountSettings(update: (ReadAccountSettingsUseCase.AccountSettings) -> ReadAccountSettingsUseCase.AccountSettings) {
        val characterId = state.value.selectedCharacterId ?: return
        _state.update { state ->
            val settings = state.displayedSettings[characterId] ?: return@update state
            val accountSettings = settings.accountSettings ?: return@update state
            state.copy(changedSettings = state.changedSettings + (characterId to settings.copy(accountSettings = update(accountSettings))))
        }
    }

    private fun updateNeocomButtons(
        characterId: Int,
        update: (List<ReadCharacterSettingsUseCase.NeocomButton>) -> List<ReadCharacterSettingsUseCase.NeocomButton>,
    ) {
        _state.update { state ->
            val currentSettings = state.displayedSettings[characterId] ?: return@update state
            val characterSettings = currentSettings.characterSettings ?: return@update state
            state.copy(changedSettings = state.changedSettings + (characterId to currentSettings.copy(
                characterSettings = characterSettings.copy(neocomButtons = update(characterSettings.neocomButtons)),
            )))
        }
    }

    private fun updateButtonColor(
        buttons: List<ReadCharacterSettingsUseCase.NeocomButton>,
        buttonId: String,
        colorId: Int?,
    ): List<ReadCharacterSettingsUseCase.NeocomButton> = buttons.map { button ->
        if (button.id == buttonId) button.copy(colorId = colorId)
        else button.copy(children = updateButtonColor(button.children, buttonId, colorId))
    }

    private fun reorderChildren(
        buttons: List<ReadCharacterSettingsUseCase.NeocomButton>,
        parentId: String,
        fromIndex: Int,
        toIndex: Int,
    ): List<ReadCharacterSettingsUseCase.NeocomButton> = buttons.map { button ->
        if (button.id == parentId) button.copy(children = button.children.move(fromIndex, toIndex))
        else button.copy(children = reorderChildren(button.children, parentId, fromIndex, toIndex))
    }

    private fun List<ReadCharacterSettingsUseCase.NeocomButton>.containsNeocomButton(buttonId: String): Boolean {
        return any { it.id == buttonId || it.children.containsNeocomButton(buttonId) }
    }

    private fun List<ReadCharacterSettingsUseCase.NeocomButton>.removeNeocomButton(
        buttonId: String,
    ): List<ReadCharacterSettingsUseCase.NeocomButton> = filterNot { it.id == buttonId }.map { button ->
        button.copy(children = button.children.removeNeocomButton(buttonId))
    }

    private fun <T> List<T>.move(fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex !in indices || toIndex !in indices) return this
        return toMutableList().also { buttons ->
            buttons.add(toIndex, buttons.removeAt(fromIndex))
        }
    }

    private fun updateWatchlist(characterId: Int, watchlist: Map<Int, Color?>) {
        _state.update { state ->
            val currentSettings = state.displayedSettings[characterId] ?: return@update state
            val characterSettings = currentSettings.characterSettings ?: return@update state
            state.copy(changedSettings = state.changedSettings + (characterId to currentSettings.copy(
                characterSettings = characterSettings.copy(watchlistColors = watchlist),
            )))
        }
    }

    private fun getAccountSettingsFile(
        character: CharacterItem,
        profile: String?,
        accounts: List<GetAccountsUseCase.Account>,
    ): Path? {
        if (profile == null) return null
        return character.accountSettingsFiles[profile]
            ?: accounts.firstOrNull { it.id == character.accountId }?.paths?.get(profile)
    }

    private fun getChangedCharacterSections(
        original: ReadCharacterSettingsUseCase.CharacterSettings?,
        changed: ReadCharacterSettingsUseCase.CharacterSettings,
    ): Set<CharacterSection> = buildSet {
        if (original == null || original.openWindows != changed.openWindows ||
            original.minimizedWindows != changed.minimizedWindows || original.windowStacks != changed.windowStacks ||
            original.windowSizesAndPositions != changed.windowSizesAndPositions ||
            original.shipUiLeftOffsetPx != changed.shipUiLeftOffsetPx || original.neocomWidthPx != changed.neocomWidthPx
        ) add(CharacterSection.WindowLayout)
        if (original == null || original.joinedChatChannels != changed.joinedChatChannels) add(CharacterSection.ChatChannels)
        if (original == null || original.neocomButtons != changed.neocomButtons || original.neocomIconColors != changed.neocomIconColors) {
            add(CharacterSection.NeocomButtons)
        }
        if (original == null || original.watchlistColors != changed.watchlistColors) add(CharacterSection.FleetWatchlist)
    }

    private fun getChangedAccountSections(
        original: ReadAccountSettingsUseCase.AccountSettings?,
        changed: ReadAccountSettingsUseCase.AccountSettings,
    ): Set<AccountSection> = buildSet {
        if (original == null || original.isShipUiOnTop != changed.isShipUiOnTop ||
            original.targetOrigin != changed.targetOrigin || original.isTargetsAlignHorizontal != changed.isTargetsAlignHorizontal
        ) add(AccountSection.WindowLayout)
        if (original == null || original.probeFormations != changed.probeFormations) add(AccountSection.ProbeFormations)
    }
}
