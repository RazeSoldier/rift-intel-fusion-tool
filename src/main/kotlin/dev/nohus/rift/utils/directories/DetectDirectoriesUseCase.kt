package dev.nohus.rift.utils.directories

import dev.nohus.rift.characters.files.DetectEveSettingsDirectoryUseCase
import dev.nohus.rift.launcher.DetectLauncherLogsDirectoryUseCase
import dev.nohus.rift.logs.DetectLogsDirectoryUseCase
import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single

@Single
class DetectDirectoriesUseCase(
    private val detectLogsDirectoryUseCase: DetectLogsDirectoryUseCase,
    private val detectLauncherLogsDirectoryUseCase: DetectLauncherLogsDirectoryUseCase,
    private val detectEveSettingsDirectoryUseCase: DetectEveSettingsDirectoryUseCase,
    private val detectEveSharedCacheDirectoryUseCase: DetectEveSharedCacheDirectoryUseCase,
    private val settings: Settings,
) {

    operator fun invoke() {
        if (settings.eveLogsDirectory == null) detectLogsDirectoryUseCase()
        if (settings.launcherLogsDirectory == null) detectLauncherLogsDirectoryUseCase()
        if (settings.eveSettingsDirectory == null) detectEveSettingsDirectoryUseCase()
        if (settings.eveSharedCacheDirectory == null) detectEveSharedCacheDirectoryUseCase()
    }
}
