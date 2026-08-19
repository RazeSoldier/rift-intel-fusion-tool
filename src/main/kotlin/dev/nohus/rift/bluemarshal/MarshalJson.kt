package dev.nohus.rift.bluemarshal

import java.math.BigInteger
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/** Lossless, human-editable JSON mapping used by Blue Marshal. */
object BlueMarshalJson {
    val parser = Json { prettyPrint = true }

    fun encode(value: MarshalValue): JsonElement = when (value) {
        MarshalValue.None -> JsonNull
        is MarshalValue.Bool -> JsonPrimitive(value.value)
        is MarshalValue.Int -> JsonPrimitive(value.value)
        is MarshalValue.Long -> JsonPrimitive("long:${value.value}")
        is MarshalValue.Float -> if (value.value.isFinite()) JsonPrimitive(value.value) else JsonPrimitive(float(value.value))
        is MarshalValue.Bytes -> JsonPrimitive(bytes(value.value))
        is MarshalValue.String -> JsonPrimitive("utf8:${value.value}")
        is MarshalValue.Global -> JsonPrimitive("global:${value.name}")
        is MarshalValue.List -> buildJsonArray { value.items.forEach { add(encode(it)) } }
        is MarshalValue.Tuple -> buildJsonObject { put("tuple", buildJsonArray { value.items.forEach { add(encode(it)) } }) }
        is MarshalValue.Dict -> buildJsonObject { value.pairs.forEach { (key, item) -> put(key(key), encode(item)) } }
        is MarshalValue.Instance -> buildJsonObject { put("instance", buildJsonObject { put("class", value.className); put("state", encode(value.state)) }) }
        is MarshalValue.Reduce -> buildJsonObject {
            put("reduce", buildJsonObject {
                put("newobj", value.newObject)
                put("callable", encode(value.callable))
                put("args", encode(value.arguments))
                put("state", value.state?.let(::encode) ?: JsonNull)
                put("list_items", buildJsonArray { value.listItems.forEach { add(encode(it)) } })
                put("dict_items", buildJsonArray { value.dictItems.forEach { (key, item) -> add(buildJsonArray { add(encode(key)); add(encode(item)) }) } })
            })
        }
        is MarshalValue.Callback -> buildJsonObject { put("callback", encode(value.value)) }
    }

    @Throws(MarshalException.Json::class)
    fun decode(json: JsonElement): MarshalValue = when (json) {
        JsonNull -> MarshalValue.None
        is JsonArray -> MarshalValue.List(json.map(::decode))
        is JsonObject -> decodeObject(json)
        is JsonPrimitive -> when {
            json.isString -> decodeString(json.content)
            json.content == "true" || json.content == "false" -> MarshalValue.Bool(json.boolean)
            json.longOrNull != null -> MarshalValue.Int(json.long)
            json.doubleOrNull != null -> MarshalValue.Float(json.double)
            else -> throw MarshalException.Json("unrepresentable number ${json.content}")
        }
        else -> throw MarshalException.Json("unknown JSON value")
    }

    fun stringify(value: MarshalValue): kotlin.String = parser.encodeToString(JsonElement.serializer(), encode(value))
    fun parse(text: kotlin.String): MarshalValue = decode(parser.parseToJsonElement(text))

    private fun decodeObject(objectValue: JsonObject): MarshalValue {
        if (objectValue.size == 1) {
            (objectValue["tuple"] as? JsonArray)?.let { return MarshalValue.Tuple(it.map(::decode)) }
            (objectValue["instance"] as? JsonObject)?.let {
                val className = it["class"]?.jsonPrimitive?.contentOrNull ?: throw MarshalException.Json("instance missing class")
                val state = it["state"] ?: throw MarshalException.Json("instance missing state")
                return MarshalValue.Instance(className, decode(state))
            }
            (objectValue["reduce"] as? JsonObject)?.let { return reduce(it) }
            objectValue["callback"]?.let { return MarshalValue.Callback(decode(it)) }
        }
        return MarshalValue.Dict(objectValue.map { (key, value) -> decodeString(key) to decode(value) })
    }

    private fun reduce(value: JsonObject): MarshalValue.Reduce {
        val callable = value["callable"] ?: throw MarshalException.Json("reduce missing callable")
        val arguments = value["args"] ?: throw MarshalException.Json("reduce missing args")
        val state = value["state"].takeUnless { it == null || it == JsonNull }?.let(::decode)
        val listItems = (value["list_items"] as? JsonArray)?.map(::decode).orEmpty()
        val dictItems = (value["dict_items"] as? JsonArray)?.map { entry ->
            val pair = entry as? JsonArray ?: throw MarshalException.Json("bad dict_items entry")
            if (pair.size != 2) throw MarshalException.Json("bad dict_items entry")
            decode(pair[0]) to decode(pair[1])
        }.orEmpty()
        return MarshalValue.Reduce(value["newobj"]?.jsonPrimitive?.booleanOrNull ?: false, decode(callable), decode(arguments), state, listItems, dictItems)
    }

    private fun float(value: Double) = when {
        value.isNaN() -> "float:nan"
        value == Double.POSITIVE_INFINITY -> "float:inf"
        value == Double.NEGATIVE_INFINITY -> "float:-inf"
        else -> "float:$value"
    }
    private fun bytes(value: ByteArray): kotlin.String {
        val text = value.toString(Charsets.UTF_8)
        return if (text.toByteArray().contentEquals(value) && !text.startsWith("b64:")) "bytes:$text" else "bytes:b64:${Base64.getEncoder().encodeToString(value)}"
    }
    private fun key(value: MarshalValue): kotlin.String = when (value) {
        MarshalValue.None -> "none"
        is MarshalValue.Bool -> "bool:${value.value}"
        is MarshalValue.Int -> "int:${value.value}"
        is MarshalValue.Float -> float(value.value)
        is MarshalValue.Long -> "long:${value.value}"
        is MarshalValue.String -> "utf8:${value.value}"
        is MarshalValue.Bytes -> bytes(value.value)
        is MarshalValue.Global -> "global:${value.name}"
        else -> "json:${parser.encodeToString(JsonElement.serializer(), encode(value))}"
    }
    private fun decodeString(value: kotlin.String): MarshalValue = when {
        value.startsWith("long:") -> try { MarshalValue.Long(BigInteger(value.removePrefix("long:"))) } catch (_: NumberFormatException) { throw MarshalException.Json("bad long literal") }
        value.startsWith("bytes:b64:") -> try { MarshalValue.Bytes(Base64.getDecoder().decode(value.removePrefix("bytes:b64:"))) } catch (_: IllegalArgumentException) { throw MarshalException.Json("bad base64 bytes") }
        value.startsWith("bytes:") -> MarshalValue.Bytes(value.removePrefix("bytes:").toByteArray())
        value.startsWith("utf8:") -> MarshalValue.String(value.removePrefix("utf8:"))
        value.startsWith("global:") -> MarshalValue.Global(value.removePrefix("global:"))
        value.startsWith("float:") -> MarshalValue.Float(when (val literal = value.removePrefix("float:")) { "nan" -> Double.NaN; "inf" -> Double.POSITIVE_INFINITY; "-inf" -> Double.NEGATIVE_INFINITY; else -> literal.toDoubleOrNull() ?: throw MarshalException.Json("bad float literal") })
        value == "none" -> MarshalValue.None
        value == "bool:true" -> MarshalValue.Bool(true)
        value == "bool:false" -> MarshalValue.Bool(false)
        value.startsWith("bool:") -> throw MarshalException.Json("bad bool literal")
        value.startsWith("int:") -> MarshalValue.Int(value.removePrefix("int:").toLongOrNull() ?: throw MarshalException.Json("bad int literal"))
        value.startsWith("json:") -> decode(parser.parseToJsonElement(value.removePrefix("json:")))
        else -> throw MarshalException.Json("string without a recognized prefix")
    }
}
