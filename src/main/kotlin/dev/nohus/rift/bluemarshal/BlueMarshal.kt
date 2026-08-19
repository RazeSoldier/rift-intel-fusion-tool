package dev.nohus.rift.bluemarshal

import kotlinx.serialization.json.JsonElement

/** Decodes one Blue Marshal stream. */
fun decode(bytes: ByteArray): DecodedMarshal = BlueMarshal.decode(bytes)

/** Encodes a value as a Blue Marshal stream. */
fun encode(value: MarshalValue, options: EncodeOptions = EncodeOptions()): ByteArray = BlueMarshal.encode(value, options)

/** Converts a marshal value to its lossless JSON representation. */
fun toJson(value: MarshalValue): JsonElement = BlueMarshalJson.encode(value)

/** Converts a lossless JSON representation back to a marshal value. */
fun fromJson(value: JsonElement): MarshalValue = BlueMarshalJson.decode(value)
