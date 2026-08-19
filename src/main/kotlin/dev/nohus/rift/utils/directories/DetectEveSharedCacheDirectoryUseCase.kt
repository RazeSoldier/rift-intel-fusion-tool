package dev.nohus.rift.utils.directories

import dev.nohus.rift.settings.persistence.Settings
import org.koin.core.annotation.Single
import java.nio.file.Path

@Single
class DetectEveSharedCacheDirectoryUseCase(
    private val getEveSharedCacheDirectoryUseCase: GetEveSharedCacheDirectoryUseCase,
    private val settings: Settings,
) {

    operator fun invoke(): Path? {
        val directory = getEveSharedCacheDirectoryUseCase()
        if (directory != null) settings.eveSharedCacheDirectory = directory
        return directory
    }
}
