package dev.nohus.rift.charactersettings.io

import dev.nohus.rift.bluemarshal.BlueMarshal
import dev.nohus.rift.bluemarshal.BlueMarshalJson
import dev.nohus.rift.bluemarshal.EncodeOptions
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

internal fun Path.readEveSettings(): JsonObject {
    val decoded = BlueMarshal.decode(readBytes())
    return BlueMarshalJson.encode(decoded.value) as JsonObject
}

internal fun Path.updateEveSettings(update: (JsonObject) -> JsonObject) {
    val decoded = BlueMarshal.decode(readBytes())
    val root = BlueMarshalJson.encode(decoded.value) as JsonObject
    val updated = BlueMarshalJson.decode(update(root))
    writeBytes(BlueMarshal.encode(updated, EncodeOptions(checksum = decoded.hadChecksum)))
}

internal fun JsonObject.section(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.updateSection(key: String, update: (JsonObject) -> JsonObject): JsonObject {
    val section = section(key) ?: return this
    return JsonObject(this + (key to update(section)))
}

internal fun JsonObject.tupleValue(key: String): JsonElement? = this[key].tupleValue()

internal fun JsonElement?.tupleValue(): JsonElement? =
    (this as? JsonObject)?.get("tuple")?.let { it as? JsonArray }?.getOrNull(1)

internal fun JsonElement?.tupleValues(): JsonArray? =
    (this as? JsonObject)?.get("tuple") as? JsonArray

internal fun JsonObject.replaceTupleValue(key: String, value: JsonElement): JsonObject {
    val replacement = this[key]?.replaceTupleValue(value) ?: storedTuple(value)
    return JsonObject(this + (key to replacement))
}

internal fun JsonElement.replaceTupleValue(value: JsonElement): JsonElement {
    if (tupleValues() == null) return this
    return storedTuple(value)
}

internal fun storedTuple(value: JsonElement): JsonObject = rawTuple(eveTimestamp(), value)

internal fun rawTuple(vararg values: JsonElement): JsonObject =
    JsonObject(mapOf("tuple" to JsonArray(values.toList())))

private fun eveTimestamp(): JsonPrimitive {
    val windowsEpochOffset = 116_444_736_000_000_000L
    val timestamp = Instant.now().toEpochMilli() * 10_000 + windowsEpochOffset
    return JsonPrimitive("long:$timestamp")
}
