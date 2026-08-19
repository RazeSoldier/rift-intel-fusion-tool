package dev.nohus.rift.charactersettings.io

import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

@Single
class GetEveSettingsBackupsUseCase(
    private val settings: Settings,
    private val accountAssociationsRepository: AccountAssociationsRepository,
) {

    data class Backup(
        val id: Int,
        val name: String,
        val characterId: Int,
        val accountId: Int,
        val profile: String,
        val characterFile: Path,
        val accountFile: Path,
        val timestamp: Instant,
    )

    private val characterRegex = """core_char_([0-9]+)_rift_backup_(.+)""".toRegex()

    operator fun invoke(): List<Backup> {
        val directory = settings.eveSettingsDirectory ?: return emptyList()
        val associations = accountAssociationsRepository.getAssociations()
        return try {
            directory.listDirectoryEntries()
                .filter { it.isDirectory() && it.name.startsWith("settings_") }
                .flatMap { profileDirectory ->
                    val files = profileDirectory.listDirectoryEntries()
                        .filter { it.isRegularFile() && it.extension == "dat" }
                    files.mapNotNull { characterFile ->
                        val match = characterRegex.matchEntire(characterFile.nameWithoutExtension) ?: return@mapNotNull null
                        val characterId = match.groupValues[1].toInt()
                        val suffix = match.groupValues[2]
                        val associatedAccountId = associations[characterId]
                        val matchingAccountFiles = files.filter {
                            it.nameWithoutExtension.matches(Regex("core_user_[0-9]+_rift_backup_${Regex.escape(suffix)}"))
                        }
                        val accountFile = matchingAccountFiles.firstOrNull {
                            associatedAccountId != null && it.nameWithoutExtension.startsWith("core_user_${associatedAccountId}_")
                        } ?: matchingAccountFiles.singleOrNull() ?: return@mapNotNull null
                        val accountId = accountFile.nameWithoutExtension.substringAfter("core_user_").substringBefore('_').toInt()
                        val profile = profileDirectory.name.substringAfter("settings_")
                        val key = "$profile:$characterId:$accountId:$suffix"
                        Backup(
                            id = key.hashCode().let { if (it > 0) -it else it }.takeIf { it != characterId } ?: Int.MIN_VALUE,
                            name = suffix.substringAfterLast("_rift_backup_").replace('_', ' '),
                            characterId = characterId,
                            accountId = accountId,
                            profile = profile,
                            characterFile = characterFile,
                            accountFile = accountFile,
                            timestamp = maxOf(
                                characterFile.getLastModifiedTime().toInstant(),
                                accountFile.getLastModifiedTime().toInstant(),
                            ),
                        )
                    }
                }
                .sortedByDescending { it.timestamp }
        } catch (e: IOException) {
            logger.error(e) { "Failed reading settings backups" }
            emptyList()
        }
    }
}
