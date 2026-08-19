package dev.nohus.rift.utils.directories

import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.osdirectories.OperatingSystemDirectories
import org.koin.core.annotation.Single
import java.nio.file.Path

@Single
class GetEveSharedCacheDirectoryUseCase(
    private val settings: Settings,
    private val operatingSystem: OperatingSystem,
    private val operatingSystemDirectories: OperatingSystemDirectories,
    private val getLinuxSteamLibrariesUseCase: GetLinuxSteamLibrariesUseCase,
    private val isEveSharedCacheDirectoryValidUseCase: IsEveSharedCacheDirectoryValidUseCase,
) {

    operator fun invoke(): Path? {
        val candidates = when (operatingSystem) {
            OperatingSystem.Linux -> getLinuxSteamLibrariesUseCase().map { library ->
                library.resolve("steamapps/compatdata/8500/pfx/drive_c/CCP/EVE Online")
            }
            OperatingSystem.Windows -> listOfNotNull(
                settings.eveSettingsDirectory?.root?.resolve("CCP/EVE Online"),
                settings.eveSettingsDirectory?.root?.resolve("CCP/EVE"),
            )
            OperatingSystem.MacOs -> {
                val home = operatingSystemDirectories.getUserDirectory()
                return home.resolve("Library/Application Support/EVE Online/SharedCache")
            }
        }
        return candidates.firstOrNull { isEveSharedCacheDirectoryValidUseCase(it) }
    }
}
