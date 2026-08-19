package dev.nohus.rift.whatsnew

import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.directories.AppDirectories
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.pathString

private val logger = KotlinLogging.logger {}

class MigrateAppDataUseCase(
    private val operatingSystem: OperatingSystem,
    private val appDirectories: AppDirectories,
) {

    private companion object {
        const val MIGRATION_FLAG_FILE = ".appdata_migration_done"
    }

    operator fun invoke() {
        try {
            if (operatingSystem != OperatingSystem.Windows) return

            val newAppData = appDirectories.getAppDataDirectory()
            val markerFile = newAppData.resolve(MIGRATION_FLAG_FILE)

            if (Files.exists(markerFile)) {
                logger.info { "AppData migration already completed, skipping" }
                return
            }

            val appDataEnv = System.getenv("AppData")
                ?: throw IllegalStateException("AppData environment variable is missing")

            val oldAppData = Path.of(appDataEnv).resolve("RIFT")

            if (!oldAppData.pathString.contains("36690Nohus.RIFTIntelFusionTool")) {
                logger.info { "App data is reported as: $oldAppData, not in MSIX sandbox, not migrating" }
                return
            }

            logger.info { "Copying data from $oldAppData to $newAppData" }

            copyAllFiles(oldAppData, newAppData)

            Files.writeString(markerFile, "migrated")

            logger.info { "Migration completed and flag file created: $markerFile" }
        } catch (e: Exception) {
            logger.error(e) { "Error when migrating data: ${e.message}" }
        }
    }

    private fun copyAllFiles(sourceDir: Path, targetDir: Path) {
        Files.createDirectories(targetDir)
        logger.info { "Ensured target directory exists: $targetDir" }

        Files.walk(sourceDir).use { paths ->
            paths.forEach { path ->
                if (path == sourceDir) return@forEach

                if (path.fileName.toString() == "diagnostics") {
                    // Don't copy over logs
                    return@forEach
                }

                val relative = sourceDir.relativize(path)
                val targetPath = targetDir.resolve(relative)

                if (Files.isDirectory(path)) {
                    Files.createDirectories(targetPath)
                } else {
                    Files.createDirectories(targetPath.parent)
                    Files.copy(
                        path,
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES
                    )
                }
            }
        }

        logger.info { "File copy completed successfully" }
    }
}
