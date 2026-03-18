package dev.nohus.rift.network.esi

import dev.nohus.rift.utils.directories.AppDirectories
import okhttp3.Cache
import org.koin.core.annotation.Single
import java.text.DateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter

@Single
class EsiCache(
    appDirectories: AppDirectories,
) {

    private val directory = appDirectories.getAppCacheDirectory().resolve("esi-cache")
    private val size = 100L * 1024 * 1024 // 100MB
    val cache = Cache(directory.toFile(), size)

    fun getDebugInfo(): String {
        val writability = if (cache.directory.canWrite()) "writable" else "read-only"
        val closed = if (cache.isClosed) "closed" else "open"
        val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        return "Cache Debug (" +
            "requests: ${cache.requestCount()}, ${cache.networkCount()} network, ${cache.hitCount()} hits; " +
            "size: ${cache.size()}/${cache.maxSize()}; " +
            "writes: ${cache.writeSuccessCount()} success, ${cache.writeAbortCount()} aborted; " +
            "filesystem: ${cache.directory.freeSpace} free space, $writability, $closed; " +
            "client time: $now" +
            ")"
    }
}
