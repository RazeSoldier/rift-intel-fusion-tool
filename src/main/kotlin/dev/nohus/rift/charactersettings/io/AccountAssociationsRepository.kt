package dev.nohus.rift.charactersettings.io

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.launcher.GetLauncherLogsUseCase
import dev.nohus.rift.launcher.ParseLauncherAccountAssociationsUseCase
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

private val logger = KotlinLogging.logger {}

@Single
class AccountAssociationsRepository(
    private val settings: Settings,
    private val localCharactersRepository: LocalCharactersRepository,
    private val getLauncherLogsUseCase: GetLauncherLogsUseCase,
    private val parseLauncherAccountAssociationsUseCase: ParseLauncherAccountAssociationsUseCase,
) {

    private val scope = CoroutineScope(Job())

    fun onCharacterLogin(characterId: Int) {
        scope.launch(Dispatchers.IO) {
            getLauncherLogsUseCase(settings.launcherLogsDirectory).firstOrNull()?.let { latestLog ->
                parseLauncherAccountAssociationsUseCase(latestLog)
                    .lastOrNull { it.characterId == characterId }
                    ?.let { association ->
                        associate(association.characterId, association.accountId)
                    }
            }
        }
    }

    fun onLauncherLogsDirectoryChanged() {
        scope.launch(Dispatchers.IO) {

            getLauncherLogsUseCase(settings.launcherLogsDirectory)
                .asReversed()
                .flatMap { logFile -> parseLauncherAccountAssociationsUseCase(logFile) }
                .groupBy({ it.characterId }, { it.accountId })
                .map { (characterId, accountIds) -> characterId to accountIds.last() }
                .forEach { (characterId, accountId) ->
                    associate(characterId, accountId)
                }
        }
    }

    /**
     * Returns a map of Character ID -> Account ID
     */
    fun getAssociations(): Map<Int, Int> = settings.accountAssociations

    fun associate(characterId: Int, accountId: Int) {
        if (settings.accountAssociations[characterId] != accountId) {
            settings.accountAssociations += characterId to accountId
            val characterName = localCharactersRepository.characters.value.firstOrNull { it.characterId == characterId }?.info?.name
            logger.info { "Set account association for character ${characterName ?: characterId} to ${accountId}." }
        }
    }
}
