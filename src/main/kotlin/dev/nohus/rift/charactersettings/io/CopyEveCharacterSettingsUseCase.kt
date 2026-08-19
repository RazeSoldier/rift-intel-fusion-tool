package dev.nohus.rift.charactersettings.io

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

private val logger = KotlinLogging.logger {}

@Single
class CopyEveCharacterSettingsUseCase(
    private val localCharactersRepository: LocalCharactersRepository,
    private val accountAssociationsRepository: AccountAssociationsRepository,
    private val getAccounts: GetAccountsUseCase,
) {

    operator fun invoke(
        fromCharacterId: Int,
        toCharacterIds: List<Int>,
        sourceLauncherProfile: String,
        targetLauncherProfile: String,
    ): Boolean {
        val characters = localCharactersRepository.characters.value
        val accounts = getAccounts()
        val accountAssociations = accountAssociationsRepository.getAssociations()

        val fromAccountId = accountAssociations[fromCharacterId] ?: run {
            logger.error { "Source character doesn't have an account set" }
            return false
        }
        val toAccountIds = toCharacterIds.map { toCharacterId ->
            accountAssociations[toCharacterId] ?: run {
                logger.error { "Target character doesn't have an account set" }
                return false
            }
        }
        logger.info { "$fromCharacterId -> $toCharacterIds, $sourceLauncherProfile -> $targetLauncherProfile" }

        val fromCharacterFile = characters.firstOrNull {
            it.characterId == fromCharacterId
        }?.settingsFiles?.get(sourceLauncherProfile) ?: run {
            logger.error { "Source character doesn't have a settings file set" }
            return false
        }
        val toCharacterFiles = toCharacterIds.map { toCharacterId ->
            fromCharacterFile.parent.parent.resolve("settings_$targetLauncherProfile").resolve("core_char_$toCharacterId.dat")
        }.distinct()
        val fromAccountFile = accounts.firstOrNull { it.id == fromAccountId }?.paths?.get(sourceLauncherProfile) ?: run {
            logger.error { "Source account doesn't have a settings file" }
            return false
        }
        val toAccountFiles = toAccountIds.map { toAccountId ->
            fromCharacterFile.parent.parent.resolve("settings_$targetLauncherProfile").resolve("core_user_$toAccountId.dat")
        }.distinct()

        logger.info { "From account: $fromAccountFile\nTo account: $toAccountFiles\nFrom char: $fromCharacterFile\nTo char: $toCharacterFiles" }

        if (!replicate(fromCharacterFile, toCharacterFiles)) return false
        if (!replicate(fromAccountFile, toAccountFiles)) return false
        return true
    }

    fun copyFiles(
        fromCharacterFile: Path,
        fromAccountFile: Path,
        toCharacterFiles: List<Path>,
        toAccountFiles: List<Path>,
    ): Boolean {
        if (!replicate(fromCharacterFile, toCharacterFiles.distinct())) return false
        if (!replicate(fromAccountFile, toAccountFiles.distinct())) return false
        return true
    }

    private fun replicate(
        fromFile: Path,
        toFiles: List<Path>,
    ): Boolean {
        try {
            if (!fromFile.exists()) {
                logger.error { "Source settings file does not exist" }
                return false
            }
            toFiles.filter { it != fromFile }.forEach { toFile ->
                toFile.deleteIfExists()
                fromFile.copyTo(toFile)
            }

            return true
        } catch (e: IOException) {
            logger.error(e) { "Copying settings failed" }
            return false
        }
    }

}
