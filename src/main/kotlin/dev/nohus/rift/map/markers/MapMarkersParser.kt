package dev.nohus.rift.map.markers

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.allDrawableResources
import dev.nohus.rift.repositories.SolarSystemsRepository
import org.koin.core.annotation.Factory
import kotlin.math.roundToInt

@Factory
class MapMarkersParser(
    private val solarSystemsRepository: SolarSystemsRepository,
) {

    data class ImportedMarker(
        val systemId: Int,
        val systemName: String,
        val label: String,
        val color: Color?,
        val icon: String,
        val isPinned: Boolean,
        val group: String?,
    )

    sealed interface ParsingResult {
        data object Empty : ParsingResult
        data class Parsed(val lines: List<ParsedLine>, val markers: List<ImportedMarker>) : ParsingResult
    }

    sealed class ParsedLine(open val text: String) {
        data class Marker(override val text: String, val marker: ImportedMarker) : ParsedLine(text)
        data class Invalid(override val text: String, val reason: String) : ParsedLine(text)
    }

    fun parse(text: String): ParsingResult {
        if (text.isBlank()) return ParsingResult.Empty

        val lines = text.lines().map { line ->
            parseLine(line)
        }
        val markers = lines.filterIsInstance<ParsedLine.Marker>().map { it.marker }
        return ParsingResult.Parsed(lines, markers)
    }

    fun format(markers: List<MapMarkerItem>): String {
        return markers.joinToString("\n") { marker ->
            listOf(
                marker.systemName,
                marker.label,
                marker.color?.toHexString().orEmpty(),
                marker.iconName.removePrefix(ICON_PREFIX),
                marker.isPinned.toString(),
                marker.group.orEmpty(),
            ).joinToString(";")
        }
    }

    private fun parseLine(line: String): ParsedLine {
        if (line.count { it == ';' } != SEPARATOR_COUNT) {
            return ParsedLine.Invalid(line, "Expected 6 fields separated by 5 semicolons")
        }
        val fields = line.split(';', limit = FIELD_COUNT)
        val systemText = fields[0]
        val label = fields[1]
        val colorText = fields[2]
        val iconText = fields[3]
        val pinnedText = fields[4]
        val groupText = fields[5]
        val system = solarSystemsRepository.getSystem(systemText.trim())
            ?: return ParsedLine.Invalid(line, "Unknown system: ${systemText.ifBlank { "(empty)" }}")
        val color = when {
            colorText.isBlank() -> null
            ColorRegex.matches(colorText.trim()) -> Color(("FF" + colorText.trim().drop(1)).toLong(16))
            else -> return ParsedLine.Invalid(line, "Color must use #RRGGBB format")
        }
        val iconName = if (iconText.isBlank()) DEFAULT_ICON else ICON_PREFIX + iconText.trim()
        if (iconName !in Res.allDrawableResources) {
            return ParsedLine.Invalid(line, "Unknown marker icon: ${iconText.ifBlank { "(empty)" }}")
        }
        val isPinned = when {
            pinnedText.isBlank() -> false
            pinnedText == "true" -> true
            pinnedText == "false" -> false
            else -> return ParsedLine.Invalid(line, "Pinned must be true or false")
        }
        val marker = ImportedMarker(
            systemId = system.id,
            systemName = system.name,
            label = label,
            color = color,
            icon = iconName,
            isPinned = isPinned,
            group = groupText.ifBlank { null },
        )
        return ParsedLine.Marker(line, marker)
    }

    private fun Color.toHexString(): String {
        val red = (red * 255).roundToInt().coerceIn(0, 255)
        val green = (green * 255).roundToInt().coerceIn(0, 255)
        val blue = (blue * 255).roundToInt().coerceIn(0, 255)
        return "#%02X%02X%02X".format(red, green, blue)
    }

    private companion object {
        const val FIELD_COUNT = 6
        const val SEPARATOR_COUNT = FIELD_COUNT - 1
        const val ICON_PREFIX = "map_marker_"
        const val DEFAULT_ICON = "map_marker_place_bookmark"
        val ColorRegex = Regex("#[0-9a-fA-F]{6}")
    }
}
