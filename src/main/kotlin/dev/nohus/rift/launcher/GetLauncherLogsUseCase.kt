package dev.nohus.rift.launcher

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

@Single
class GetLauncherLogsUseCase {

    private val regex = """eve-online-launcher-[\d.-]+""".toRegex()

    operator fun invoke(launcherLogsDirectory: Path?): List<Path> {
        if (launcherLogsDirectory == null) return emptyList()
        return try {
            if (!launcherLogsDirectory.exists()) return emptyList()
            launcherLogsDirectory.listDirectoryEntries()
                .filter { file -> file.isRegularFile() && file.extension == "log" }
                .filter { file -> file.nameWithoutExtension.matches(regex) }
                .sortedByDescending { it.getLastModifiedTime() }
        } catch (e: IOException) {
            logger.error(e) { "Failed reading launcher logs" }
            emptyList()
        }
    }
}
