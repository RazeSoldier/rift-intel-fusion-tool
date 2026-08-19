package dev.nohus.rift.charactersettings.io

import androidx.compose.ui.graphics.Color
import dev.nohus.rift.charactersettings.compose.NeocomButtonDefinitionsById
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

@Single
class ReadCharacterSettingsUseCase {

    data class CharacterSettings(
        val openWindows: List<String>,
        val minimizedWindows: List<String>,
        val windowStacks: List<Pair<String, String?>>,
        val windowSizesAndPositions: Map<String, List<Int>>,
        val screenResolution: Pair<Int, Int>,
        val joinedChatChannels: Map<String, String>,
        val neocomWidthPx: Int,
        val neocomIconColors: Map<String, Int>,
        val neocomButtons: List<NeocomButton>,
        val shipUiLeftOffsetPx: Float,
        val watchlistColors: Map<Int, Color?>,
        val rawOpenWindows: JsonObject,
        val rawMinimizedWindows: JsonObject,
        val rawWindowStacks: JsonObject,
        val rawWindowSizesAndPositions: JsonObject,
        val rawJoinedChatChannels: JsonArray,
        val parsedChatChannelIds: Set<String>,
        val rawNeocomButtons: JsonArray,
        val rawNeocomIconColors: Map<String, JsonElement>,
        val rawWatchlistColors: JsonObject,
        val parsedWatchlistColorKeys: Set<String>,
    )

    data class NeocomButton(
        val btnType: Int,
        val id: String,
        val label: String,
        val iconPath: String,
        val colorId: Int?,
        val children: List<NeocomButton>
    )

    operator fun invoke(settingsFile: Path?): CharacterSettings? {
        if (settingsFile == null) return null
        return try {
            read(settingsFile)
        } catch (e: Exception) {
            logger.error(e) { "Failed to read the settings file $settingsFile" }
            null
        }
    }

    private fun read(settingsFile: Path): CharacterSettings? {
        val settings = settingsFile.readEveSettings()

        val windows = settings.section("bytes:windows")
        val rawOpenWindows = windows?.tupleValue("bytes:openWindows") as? JsonObject ?: JsonObject(emptyMap())
        val openWindows = rawOpenWindows.entries
            .filter { runCatching { it.value.jsonPrimitive.booleanOrNull }.getOrNull() == true }.map { it.key }
        val rawMinimizedWindows = windows?.tupleValue("bytes:minimizedWindows") as? JsonObject ?: JsonObject(emptyMap())
        val minimizedWindows = rawMinimizedWindows.entries
            .filter { runCatching { it.value.jsonPrimitive.booleanOrNull }.getOrNull() == true }.map { it.key }
        val rawWindowStacks = windows?.tupleValue("bytes:stacksWindows") as? JsonObject ?: JsonObject(emptyMap())
        val windowStacks = rawWindowStacks.entries
            .mapNotNull { entry -> runCatching { entry.key to entry.value.jsonPrimitive.contentOrNull }.getOrNull() }
        val rawWindowSizesAndPositions = windows?.tupleValue("bytes:windowSizesAndPositions_1") as? JsonObject ?: JsonObject(emptyMap())
        val windowSizesAndPositions = rawWindowSizesAndPositions.entries
            .mapNotNull { entry ->
                val bounds = runCatching { entry.value.tupleValues()?.map { it.jsonPrimitive.int } }.getOrNull()
                    ?.takeIf { it.size == 6 } ?: return@mapNotNull null
                entry.key to bounds
            }
            .associate { it.first to it.second }
        val shipUiLeftOffset = windows?.tupleValue("bytes:shipuialignleftoffset")?.jsonPrimitive?.floatOrNull ?: 0f
        val resolution = windowSizesAndPositions.entries.firstOrNull()?.value?.takeLast(2)?.let { it[0] to it[1] } ?: run {
            logger.error { "No screen resolution found for $settingsFile" }
            return null
        }

        val ui = settings.section("bytes:ui")
        val rawJoinedChatChannels = ui?.tupleValue("bytes:chatchannels") as? JsonArray ?: JsonArray(emptyList())
        val joinedChatChannels = rawJoinedChatChannels.mapNotNull { channel ->
            runCatching { channel.tupleValues()?.map { value -> value.jsonPrimitive.contentOrNull } }.getOrNull()
                ?.let { values ->
                    (values.getOrNull(0)?.substringAfter(":") ?: return@let null) to
                        (values.getOrNull(2)?.substringAfter(":") ?: return@let null)
                }
        }.toMap()

        val neocomButtonColorsKeys = ui?.keys?.filter { it.startsWith("utf8:neocom_icon_color_") } ?: emptyList()
        val rawNeocomIconColors = neocomButtonColorsKeys.mapNotNull { key -> ui?.get(key)?.let { key to it } }.toMap()
        val neocomButtonColors = neocomButtonColorsKeys.mapNotNull { key ->
            val button = key.substringAfter("utf8:neocom_icon_color_")
            val colorId = ui?.tupleValue(key)?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            button to colorId
        }.toMap()

        val neocomButtonsArray = ui?.tupleValue("bytes:neocomButtonRawData") as? JsonArray ?: return null
        val neocomButtons = getNeocomButtons(neocomButtonsArray, neocomButtonColors)

        val neocomWidthPx = ui.tupleValue("bytes:neocomWidth")?.jsonPrimitive?.intOrNull ?: 48
        // Side "snap gap" is 17px

        val rawWatchlistColors = ui.tupleValue("bytes:fleet_watchlistcolors") as? JsonObject ?: JsonObject(emptyMap())
        val parsedWatchlistColors = rawWatchlistColors.entries.mapNotNull { entry ->
            val characterId = entry.key.substringAfter("int:").toIntOrNull() ?: return@mapNotNull null
            val color = runCatching {
                if (entry.value is kotlinx.serialization.json.JsonNull) {
                    null
                } else {
                    entry.value.tupleValues()?.mapNotNull { it.jsonPrimitive.floatOrNull }?.takeIf { it.size == 3 }?.let { (r, g, b) ->
                        Color(r, g, b)
                    } ?: return@runCatching null
                }
            }.getOrNull().let { parsedColor ->
                if (parsedColor == null && entry.value !is kotlinx.serialization.json.JsonNull) return@mapNotNull null
                parsedColor
            }
            entry.key to (characterId to color)
        }
        val watchlistColors = parsedWatchlistColors.associate { it.second }

        return CharacterSettings(
            openWindows = openWindows,
            minimizedWindows = minimizedWindows,
            windowStacks = windowStacks,
            windowSizesAndPositions = windowSizesAndPositions,
            screenResolution = resolution,
            joinedChatChannels = joinedChatChannels,
            neocomWidthPx = neocomWidthPx,
            neocomIconColors = neocomButtonColors,
            neocomButtons = neocomButtons,
            shipUiLeftOffsetPx = shipUiLeftOffset,
            watchlistColors = watchlistColors,
            rawOpenWindows = rawOpenWindows,
            rawMinimizedWindows = rawMinimizedWindows,
            rawWindowStacks = rawWindowStacks,
            rawWindowSizesAndPositions = rawWindowSizesAndPositions,
            rawJoinedChatChannels = rawJoinedChatChannels,
            parsedChatChannelIds = joinedChatChannels.keys,
            rawNeocomButtons = neocomButtonsArray,
            rawNeocomIconColors = rawNeocomIconColors,
            rawWatchlistColors = rawWatchlistColors,
            parsedWatchlistColorKeys = parsedWatchlistColors.map { it.first }.toSet(),
        )
    }

    private fun getNeocomButtons(buttons: JsonArray, neocomButtonColors: Map<String, Int>): List<NeocomButton> {
        return buttons.mapNotNull { element ->
            val rawButton = parseNeocomButton(element) ?: return@mapNotNull null
            val definition = NeocomButtonDefinitionsById[rawButton.id]
            val iconPath = rawButton.iconPath ?: definition?.iconPath ?: DEFAULT_NEOCOM_ICON_PATH
            val children = rawButton.children?.let { getNeocomButtons(it, neocomButtonColors) } ?: emptyList()
            NeocomButton(
                btnType = rawButton.btnType,
                id = rawButton.id,
                label = rawButton.label ?: definition?.name ?: rawButton.id,
                iconPath = iconPath,
                colorId = neocomButtonColors[rawButton.id],
                children = children,
            )
        }
    }

    private fun parseNeocomButton(element: JsonElement): RawNeocomButton? {
        val objectValue = element as? JsonObject ?: return null
        val state = (objectValue["instance"] as? JsonObject)?.get("state") as? JsonObject
        if (state != null) {
            val id = (state["bytes:id"] as? JsonPrimitive)?.contentOrNull?.substringAfter(":") ?: return null
            return RawNeocomButton(
                btnType = (state["bytes:btnType"] as? JsonPrimitive)?.intOrNull ?: 1,
                id = id,
                label = (state["bytes:label"] as? JsonPrimitive)?.contentOrNull?.substringAfter(":"),
                iconPath = (state["bytes:iconPath"] as? JsonPrimitive)?.contentOrNull,
                children = state["bytes:children"] as? JsonArray,
            )
        }

        val tuple = element.tupleValues() ?: return null
        if (tuple.size !in 3..4) return null
        val id = (tuple[1] as? JsonPrimitive)?.contentOrNull?.substringAfter(":") ?: return null
        return RawNeocomButton(
            btnType = (tuple[0] as? JsonPrimitive)?.intOrNull ?: return null,
            id = id,
            label = null,
            iconPath = tuple.getOrNull(2).takeIf { tuple.size == 4 }?.let { it as? JsonPrimitive }?.contentOrNull,
            children = tuple.lastOrNull() as? JsonArray,
        )
    }

    private data class RawNeocomButton(
        val btnType: Int,
        val id: String,
        val label: String?,
        val iconPath: String?,
        val children: JsonArray?,
    )

    private companion object {
        const val DEFAULT_NEOCOM_ICON_PATH = "bytes:res:/ui/Texture/WindowIcons/other.png"
    }
}
