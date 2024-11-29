package dev.nohus.rift.startupwarning

import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import okio.IOException
import org.koin.core.annotation.Single
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readBytes

private val logger = KotlinLogging.logger {}

@Single
class HasChatLogsDisabledUseCase(
    private val settings: Settings,
) {

    private val regex = """core_user_[0-9]+""".toRegex()

    operator fun invoke(): Boolean {
        val directory = settings.eveSettingsDirectory ?: return false
        val userSettingsFiles = directory.listDirectoryEntries()
            .filter { file ->
                file.isDirectory() && file.name.startsWith("settings_")
            }.flatMap { directory ->
                directory.listDirectoryEntries()
                    .filter { file -> file.isRegularFile() && file.extension == "dat" }
                    .filter { file -> file.nameWithoutExtension.matches(regex) }
            }
        return userSettingsFiles.any { file ->
            try {
                val bytes = file.readBytes()
                val indexOfLogChat = bytes.indexOf("logchat")
                if (indexOfLogChat != null) {
                    bytes[indexOfLogChat + CHAT_LOGS_DISABLED_BYTE_OFFSET].toInt() == CHAT_LOGS_DISABLED_BYTE_VALUE
                } else {
                    false // The setting is not set, and defaults to enabled
                }
            } catch (e: IOException) {
                logger.warn { "Could not read settings file to determine if chat logs are enabled: ${e.message}" }
                false
            }
        }
    }

    private fun ByteArray.indexOf(text: String): Int? {
        main@ for (index in indices) {
            for (characterIndex in text.indices) {
                val character = text[characterIndex].code.toByte()
                val byte = get(index + characterIndex)
                if (character != byte) continue@main
            }
            return index
        }
        return null
    }

    companion object {
        private const val CHAT_LOGS_DISABLED_BYTE_OFFSET = -3
        private const val CHAT_LOGS_DISABLED_BYTE_VALUE = 8
    }
}
