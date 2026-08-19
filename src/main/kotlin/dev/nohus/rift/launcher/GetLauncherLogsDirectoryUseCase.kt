package dev.nohus.rift.launcher

import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.GetLinuxSteamLibrariesUseCase
import dev.nohus.rift.utils.osdirectories.OperatingSystemDirectories
import org.koin.core.annotation.Single
import java.nio.file.Path
import kotlin.io.path.exists

@Single
class GetLauncherLogsDirectoryUseCase(
    private val operatingSystem: OperatingSystem,
    private val operatingSystemDirectories: OperatingSystemDirectories,
    private val getLinuxSteamLibrariesUseCase: GetLinuxSteamLibrariesUseCase,
) {

    operator fun invoke(): Path? {
        return when (operatingSystem) {
            OperatingSystem.Linux -> getLinuxLauncherLogsDirectory()
            OperatingSystem.Windows -> getWindowsLauncherLogsDirectory()
            OperatingSystem.MacOs -> getMacLauncherLogsDirectory()
        }
    }

    private fun getLinuxLauncherLogsDirectory(): Path? {
        val libraries = getLinuxSteamLibrariesUseCase()
        return libraries.map { library ->
            library.resolve("steamapps/compatdata/8500/pfx/drive_c/users/steamuser/AppData/Roaming/EVE Online/logs")
        }.firstOrNull { it.exists() }
    }

    private fun getWindowsLauncherLogsDirectory(): Path? {
        val home = operatingSystemDirectories.getUserDirectory()
        return home.resolve("AppData/Roaming/EVE Online/logs").takeIf { it.exists() }
    }

    private fun getMacLauncherLogsDirectory(): Path? {
        val home = operatingSystemDirectories.getUserDirectory()
        return home.resolve("Library/Logs/EVE Online").takeIf { it.exists() }
    }
}
