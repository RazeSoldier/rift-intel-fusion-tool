package dev.nohus.rift.launcher

import dev.nohus.rift.utils.get
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.readText

private val logger = KotlinLogging.logger {}

@Single
class ParseLauncherAccountAssociationsUseCase {

    data class LauncherAssociation(
        val accountId: Int,
        val characterId: Int,
    )

    operator fun invoke(logFile: Path): List<LauncherAssociation> {
        return try {
            readLauncherAssociations(logFile.readText())
        } catch (e: IOException) {
            logger.error(e) { "Failed reading launcher log: ${logFile.fileName}" }
            emptyList()
        }
    }

    private fun readLauncherAssociations(log: String): List<LauncherAssociation> {
        return launcherClientStartupRegex.findAll(log).map { match ->
            LauncherAssociation(
                accountId = match["userId"].toInt(),
                characterId = match["characterId"].toInt(),
            )
        }.toList()
    }

    private companion object {
        val launcherClientStartupRegex = """
            (?s)\[client-queue] (?:Queued client startup|Started client from group startup)\s+\{
            .*?\bproduct: 'eve-online',
            .*?\buserId:\s*(?<userId>\d+),
            .*?\bcharacterId:\s*(?<characterId>\d+),
            .*?
            }
        """.trimIndent().toRegex()
    }
}
