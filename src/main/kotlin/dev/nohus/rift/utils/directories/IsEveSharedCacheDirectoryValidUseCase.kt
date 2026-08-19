package dev.nohus.rift.utils.directories

import org.koin.core.annotation.Single
import java.nio.file.Path
import kotlin.io.path.exists

@Single
class IsEveSharedCacheDirectoryValidUseCase {

    operator fun invoke(directory: Path?): Boolean {
        return directory?.resolve("ResFiles")?.exists() == true
    }
}
