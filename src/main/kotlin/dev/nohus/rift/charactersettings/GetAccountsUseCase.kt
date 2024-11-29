package dev.nohus.rift.charactersettings

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

@Single
class GetAccountsUseCase(
    private val settings: Settings,
) {

    private val regex = """core_user_[0-9]+""".toRegex()

    data class Account(
        val id: Int,
        val path: Path,
        val lastModified: Instant,
    )

    operator fun invoke(): List<Account> {
        val directory = settings.eveSettingsDirectory ?: return emptyList()
        return directory.listDirectoryEntries()
            .filter { file ->
                file.isDirectory() && file.name.startsWith("settings_")
            }.flatMap { directory ->
                directory.listDirectoryEntries()
                    .filter { file -> file.isRegularFile() && file.extension == "dat" }
                    .filter { file -> file.nameWithoutExtension.matches(regex) }
            }.mapNotNull { accountFile ->
                val id = accountFile.nameWithoutExtension.substringAfterLast("_").toIntOrNull()
                    ?: return@mapNotNull null
                val lastModified = accountFile.getLastModifiedTime().toInstant()
                Account(id, accountFile, lastModified)
            }
    }
}
