package dev.nohus.rift.launcher

import dev.nohus.rift.charactersettings.io.AccountAssociationsRepository
import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single
import java.nio.file.Path

@Single
class DetectLauncherLogsDirectoryUseCase(
    private val getLauncherLogsDirectoryUseCase: GetLauncherLogsDirectoryUseCase,
    private val settings: Settings,
    private val accountAssociationsRepository: AccountAssociationsRepository,
) {

    /**
     * Detects the EVE Launcher logs directory and updates settings with it, overwriting any existing setting.
     */
    operator fun invoke(): Path? {
        val directory: Path? = getLauncherLogsDirectoryUseCase()
        if (directory != null) {
            settings.launcherLogsDirectory = directory
            accountAssociationsRepository.onLauncherLogsDirectoryChanged()
        }
        return directory
    }
}
