package dev.nohus.rift.charactersettings.io

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

@Single
class WriteEveSettingsUseCase {

    enum class CharacterSection {
        WindowLayout,
        ChatChannels,
        NeocomButtons,
        FleetWatchlist,
    }

    enum class AccountSection {
        WindowLayout,
        ProbeFormations,
    }

    fun writeCharacterSettings(
        path: Path,
        settings: ReadCharacterSettingsUseCase.CharacterSettings,
        sections: Set<CharacterSection>,
    ): Boolean = write(path) { root ->
        root
            .let { current ->
                if (CharacterSection.WindowLayout !in sections) current else current.updateSection("bytes:windows") { windows ->
                    windows
                        .replaceTupleValue("bytes:openWindows", booleanMap(settings.rawOpenWindows, settings.openWindows))
                        .replaceTupleValue("bytes:minimizedWindows", booleanMap(settings.rawMinimizedWindows, settings.minimizedWindows))
                        .replaceTupleValue("bytes:stacksWindows", JsonObject(settings.rawWindowStacks + settings.windowStacks.associate { (window, stack) ->
                            window to (stack?.let(::JsonPrimitive) ?: JsonNull)
                        }))
                        .replaceTupleValue("bytes:windowSizesAndPositions_1", JsonObject(settings.rawWindowSizesAndPositions + settings.windowSizesAndPositions.mapValues { (_, bounds) ->
                            rawTuple(*bounds.map(::JsonPrimitive).toTypedArray())
                        }))
                        .replaceTupleValue("bytes:shipuialignleftoffset", JsonPrimitive(settings.shipUiLeftOffsetPx))
                }
            }
            .updateSection("bytes:ui") { originalUi ->
                var ui = originalUi
                if (CharacterSection.WindowLayout in sections) {
                    ui = ui.replaceTupleValue("bytes:neocomWidth", JsonPrimitive(settings.neocomWidthPx))
                }
                if (CharacterSection.ChatChannels in sections) {
                    val channels = settings.rawJoinedChatChannels.filter { channel ->
                        val id = runCatching {
                            channel.tupleValues()?.getOrNull(0)?.jsonPrimitive?.contentOrNull?.substringAfter(":")
                        }.getOrNull()
                        id == null || id !in settings.parsedChatChannelIds || id in settings.joinedChatChannels
                    }
                    ui = ui.replaceTupleValue("bytes:chatchannels", JsonArray(channels))
                }
                if (CharacterSection.NeocomButtons in sections) {
                    val reorderedButtons = reorderNeocomButtons(settings.rawNeocomButtons, settings.neocomButtons)
                    val colors = (ui.filterKeys { !it.startsWith("utf8:neocom_icon_color_") } + settings.rawNeocomIconColors).toMutableMap()
                    flattenButtons(settings.neocomButtons).forEach { button ->
                        val key = "utf8:neocom_icon_color_${button.id}"
                        if (button.colorId == null) {
                            colors.remove(key)
                        } else {
                            colors[key] = settings.rawNeocomIconColors[key]?.replaceTupleValue(JsonPrimitive(button.colorId))
                                ?: storedTuple(JsonPrimitive(button.colorId))
                        }
                    }
                    ui = JsonObject(colors)
                        .replaceTupleValue("bytes:neocomButtonRawData", JsonArray(reorderedButtons))
                }
                if (CharacterSection.FleetWatchlist in sections) {
                    val colors = settings.rawWatchlistColors.filterKeys { key ->
                        val id = key.substringAfter("int:").toIntOrNull()
                        key !in settings.parsedWatchlistColorKeys || id == null || id in settings.watchlistColors
                    } + settings.watchlistColors.mapKeys { "int:${it.key}" }.mapValues { (_, color) ->
                        if (color == null) JsonNull else rawTuple(JsonPrimitive(color.red), JsonPrimitive(color.green), JsonPrimitive(color.blue))
                    }
                    ui = ui.replaceTupleValue("bytes:fleet_watchlistcolors", JsonObject(colors))
                }
                ui
            }
    }

    fun writeAccountSettings(
        path: Path,
        settings: ReadAccountSettingsUseCase.AccountSettings,
        sections: Set<AccountSection>,
    ): Boolean = write(path) { root ->
        root.updateSection("bytes:ui") { ui ->
            var updated = ui
            if (AccountSection.WindowLayout in sections) {
                updated = updated
                    .replaceTupleValue("bytes:shipuialigntop", JsonPrimitive(settings.isShipUiOnTop))
                    .replaceTupleValue("bytes:alignHorizontally", JsonPrimitive(settings.isTargetsAlignHorizontal))
                    .let { windowUi ->
                        settings.targetOrigin?.let { origin ->
                            windowUi.replaceTupleValue("bytes:targetOrigin", rawTuple(JsonPrimitive(origin.first), JsonPrimitive(origin.second)))
                        } ?: windowUi
                    }
            }
            if (AccountSection.ProbeFormations in sections) {
                val preserved = settings.rawProbeFormations.filterKeys { it !in settings.parsedProbeFormationKeys }.toMutableMap()
                settings.probeFormations.forEach { formation ->
                    val key = generateSequence(0) { it + 1 }
                        .map { "int:$it" }
                        .first { it !in preserved }
                    preserved[key] = rawTuple(
                        JsonPrimitive("utf8:${formation.name}"),
                        JsonArray(formation.probes.map { probe ->
                            rawTuple(
                                rawTuple(JsonPrimitive(probe.x), JsonPrimitive(probe.y), JsonPrimitive(probe.z)),
                                JsonPrimitive(probe.scanRadius),
                            )
                        }),
                    )
                }
                updated = updated.replaceTupleValue("bytes:probescanning.customFormations", JsonObject(preserved))
            }
            updated
        }
    }

    private fun write(path: Path, update: (JsonObject) -> JsonObject): Boolean = try {
        path.updateEveSettings(update)
        true
    } catch (e: Exception) {
        logger.error(e) { "Failed to write EVE settings file $path" }
        false
    }

    private fun booleanMap(existing: JsonObject, enabled: List<String>): JsonObject {
        val disabled = existing.mapValues { (_, value) ->
            if (runCatching { value.jsonPrimitive.booleanOrNull }.getOrNull() != null) JsonPrimitive(false) else value
        }
        return JsonObject(disabled + enabled.associateWith { JsonPrimitive(true) })
    }

    private fun reorderNeocomButtons(
        original: List<JsonElement>,
        buttons: List<ReadCharacterSettingsUseCase.NeocomButton>,
    ): List<JsonElement> {
        val byId = original.mapNotNull { element -> neocomButtonId(element)?.let { it to element } }.toMap()
        val ordered = buttons.map { button ->
            byId[button.id]?.let { updateNeocomButton(it, button) } ?: createNeocomButton(button)
        }
        return ordered + original.filter { neocomButtonId(it) == null }
    }

    private fun updateNeocomButton(
        element: JsonElement,
        button: ReadCharacterSettingsUseCase.NeocomButton,
    ): JsonElement {
        val objectValue = element as? JsonObject ?: return element
        val instance = objectValue["instance"] as? JsonObject
        val state = instance?.get("state") as? JsonObject
        if (instance != null && state != null) {
            val originalChildren = state["bytes:children"]
            val children = updatedNeocomChildren(originalChildren, button.children)
            return JsonObject(objectValue + ("instance" to JsonObject(instance + ("state" to JsonObject(state + ("bytes:children" to children))))))
        }

        val tuple = element.tupleValues() ?: return element
        if (tuple.size !in 3..4) return element
        val values = tuple.toMutableList()
        values[values.lastIndex] = updatedNeocomChildren(values.last(), button.children)
        return rawTuple(*values.toTypedArray())
    }

    private fun createNeocomButton(button: ReadCharacterSettingsUseCase.NeocomButton): JsonElement = rawTuple(
        JsonPrimitive(button.btnType),
        JsonPrimitive("bytes:${button.id}"),
        updatedNeocomChildren(null, button.children),
    )

    private fun updatedNeocomChildren(
        original: JsonElement?,
        buttons: List<ReadCharacterSettingsUseCase.NeocomButton>,
    ): JsonElement {
        return if (original is JsonArray || buttons.isNotEmpty()) {
            JsonArray(reorderNeocomButtons(original as? JsonArray ?: emptyList(), buttons))
        } else {
            JsonNull
        }
    }

    private fun neocomButtonId(element: JsonElement): String? {
        val objectValue = element as? JsonObject ?: return null
        val state = (objectValue["instance"] as? JsonObject)?.get("state") as? JsonObject
        val instanceId = (state?.get("bytes:id") as? JsonPrimitive)?.contentOrNull
        if (instanceId != null) return instanceId.substringAfter(":")

        val tuple = element.tupleValues() ?: return null
        if (tuple.size !in 3..4) return null
        return (tuple[1] as? JsonPrimitive)?.contentOrNull?.substringAfter(":")
    }

    private fun flattenButtons(buttons: List<ReadCharacterSettingsUseCase.NeocomButton>): List<ReadCharacterSettingsUseCase.NeocomButton> {
        return buttons.flatMap { listOf(it) + flattenButtons(it.children) }
    }
}
