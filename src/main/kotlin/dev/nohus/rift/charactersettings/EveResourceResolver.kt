package dev.nohus.rift.charactersettings

import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.utils.OperatingSystem
import dev.nohus.rift.utils.OperatingSystem.*
import org.koin.core.annotation.Single
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

/** Resolves EVE's `res:/` paths to files in the locally installed shared cache. */
@Single
class EveResourceResolver(
    private val settings: Settings,
    private val operatingSystem: OperatingSystem,
) {

    private var loadedCacheDirectory: Path? = null
    private var resourcePaths: Map<String, Path>? = null

    operator fun invoke(resourcePath: String): Path? {
        val cacheDirectory = settings.eveSharedCacheDirectory ?: return null
        val paths = resourcePaths?.takeIf { loadedCacheDirectory == cacheDirectory }
            ?: loadResourcePaths(cacheDirectory).also {
                loadedCacheDirectory = cacheDirectory
                resourcePaths = it
            }
        return paths[resourcePath.removePrefix("bytes:").lowercase()]
    }

    private fun loadResourcePaths(cacheDirectory: Path): Map<String, Path> {
        val resourceIndex = when (operatingSystem) {
            Linux, Windows -> cacheDirectory.resolve("tq/resfileindex.txt")
            MacOs -> cacheDirectory.resolve("tq/EVE.app/Contents/Resources/build/resfileindex.txt")
        }
        if (!resourceIndex.exists()) return emptyMap()

        return resourceIndex
            .readLines()
            .asSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val (originalPath, path) = line.split(",")
                originalPath to cacheDirectory.resolve("ResFiles").resolve(path)
            }
    }
}
