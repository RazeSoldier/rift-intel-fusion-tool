package dev.nohus.rift.charactersettings.io

import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.annotation.Single
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

private val logger = KotlinLogging.logger {}

data class EveProfileDisplaySettings(
    val displayResolution: Pair<Int, Int>,
    val uiScale: Double?,
) {
    val layoutResolution: Pair<Int, Int> = uiScale
        ?.takeIf { it > 0.0 }
        ?.let { scale ->
            (displayResolution.first / scale).toInt() to (displayResolution.second / scale).toInt()
        }
        ?: displayResolution
}

@Single
class ReadEveProfileResolutionUseCase {

    operator fun invoke(profileDirectory: Path?): EveProfileDisplaySettings? {
        if (profileDirectory == null) return null
        val profileSettingsFile = profileDirectory.resolve("core_public__.yaml")
        if (!profileSettingsFile.exists()) return null

        val yaml = runCatching { profileSettingsFile.readText() }
            .onFailure { logger.info { "Could not read EVE profile settings file $profileSettingsFile" } }
            .getOrNull() ?: return null
        val windowMode = readStoredInt(yaml, "WindowMode")
        val newSettingsKey = when (windowMode) {
            WINDOW_MODE_FULL_SCREEN -> FULL_SCREEN_SETTINGS_KEY
            WINDOW_MODE_WINDOWED -> WINDOWED_SETTINGS_KEY
            WINDOW_MODE_FIXED_WINDOW -> FIXED_WINDOW_SETTINGS_KEY
            else -> null
        }
        val displayResolution = newSettingsKey
            ?.let { readProfileSetting(yaml, it) }
            ?.let { readResolution(it) }
            ?: run {
                val hasNewSettings = windowModeSettingsKeys.values.any { key ->
                    readProfileSetting(yaml, key)?.let { hasResolutionFields(it) } == true
                }
                if (!hasNewSettings) return null
                val effectiveWindowMode = windowMode ?: WINDOW_MODE_FULL_SCREEN
                val effectiveSettingsKey = windowModeSettingsKeys[effectiveWindowMode] ?: return null
                readProfileSetting(yaml, effectiveSettingsKey)?.let { readResolution(it) }
            }
            ?: return null
        val uiScaleKey = if (windowMode == WINDOW_MODE_FULL_SCREEN || windowMode == null) {
            UI_SCALE_FULLSCREEN_KEY
        } else {
            UI_SCALE_WINDOWED_KEY
        }
        val uiScale = readStoredDouble(yaml, uiScaleKey)?.takeIf { it > 0.0 }
        return EveProfileDisplaySettings(
            displayResolution = displayResolution,
            uiScale = uiScale,
        )
    }

    private fun readProfileSetting(yaml: String, key: String): String? {
        val lines = yaml.lines()
        val keyRegex = Regex("""^(\s*)${Regex.escape(key)}:\s*(.*)$""")
        val keyLineIndex = lines.indexOfFirst { keyRegex.matches(it) }
        if (keyLineIndex == -1) return null
        val match = keyRegex.matchEntire(lines[keyLineIndex]) ?: return null
        val indentation = match.groupValues[1].length
        val settingLines = buildList {
            add(match.groupValues[2])
            for (line in lines.drop(keyLineIndex + 1)) {
                val lineIndentation = line.indexOfFirst { !it.isWhitespace() }
                val isNextSetting = lineIndentation in 0..indentation &&
                    nextSettingRegex.matches(line.trimStart())
                if (isNextSetting) break
                add(line.trim())
            }
        }
        return settingLines.joinToString(" ")
    }

    private fun readResolution(setting: String): Pair<Int, Int>? {
        val width = widthRegex.find(setting)?.groupValues?.get(1)?.toIntOrNull()
        val height = heightRegex.find(setting)?.groupValues?.get(1)?.toIntOrNull()
        if (width != null && height != null && width > 0 && height > 0) return width to height

        return resolutionTupleRegex.find(setting)?.destructured?.let { (tupleWidth, tupleHeight) ->
            tupleWidth.toInt() to tupleHeight.toInt()
        } ?: blockResolutionTupleRegex.find(setting)?.destructured?.let { (tupleWidth, tupleHeight) ->
            tupleWidth.toInt() to tupleHeight.toInt()
        }
    }

    private fun hasResolutionFields(setting: String): Boolean {
        return widthRegex.find(setting) != null && heightRegex.find(setting) != null
    }

    private fun readStoredInt(yaml: String, key: String): Int? {
        val setting = readProfileSetting(yaml, key) ?: return null
        return storedIntRegex.find(setting)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun readStoredDouble(yaml: String, key: String): Double? {
        val setting = readProfileSetting(yaml, key) ?: return null
        return storedDoubleRegex.find(setting)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private companion object {
        const val WINDOW_MODE_FULL_SCREEN = 0
        const val WINDOW_MODE_WINDOWED = 1
        const val WINDOW_MODE_FIXED_WINDOW = 2

        const val FULL_SCREEN_SETTINGS_KEY = "FullScreenSettings"
        const val WINDOWED_SETTINGS_KEY = "WindowedSettings"
        const val FIXED_WINDOW_SETTINGS_KEY = "FixedWindowSettings"
        const val UI_SCALE_FULLSCREEN_KEY = "UIScaleFullscreen"
        const val UI_SCALE_WINDOWED_KEY = "UIScaleWindowed"

        val windowModeSettingsKeys = mapOf(
            WINDOW_MODE_FULL_SCREEN to FULL_SCREEN_SETTINGS_KEY,
            WINDOW_MODE_WINDOWED to WINDOWED_SETTINGS_KEY,
            WINDOW_MODE_FIXED_WINDOW to FIXED_WINDOW_SETTINGS_KEY,
        )

        val widthRegex = Regex("""(?i)(?:BackBufferWidth|width):\s*(\d+)""")
        val heightRegex = Regex("""(?i)(?:BackBufferHeight|height):\s*(\d+)""")
        val resolutionTupleRegex = Regex("""\[\s*(\d{3,6})\s*,\s*(\d{3,6})\s*]""")
        val blockResolutionTupleRegex = Regex("""-\s*-\s*(\d{3,6})\s+-\s*(\d{3,6})""")
        val storedIntRegex = Regex("""(?:\[\s*|-\s*)\d+(?:\s*,\s*|\s+-\s*)(\d+)""")
        val storedDoubleRegex = Regex("""(?:\[\s*|-\s*)\d+(?:\s*,\s*|\s+-\s*)(\d+(?:\.\d+)?)""")
        val nextSettingRegex = Regex("""[A-Za-z][A-Za-z0-9_]*:.*""")
    }
}
