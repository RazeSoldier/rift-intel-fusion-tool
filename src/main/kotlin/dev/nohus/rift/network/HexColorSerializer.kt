package dev.nohus.rift.network

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HexColorSerializer : KSerializer<Color> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HexColor", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        val argb = value.toArgb()
        val alpha = (argb ushr 24) and 0xFF
        val hex = if (alpha == 0xFF) {
            "#%06X".format(argb and 0x00FFFFFF)
        } else {
            "#%08X".format(argb.toLong() and 0xFFFFFFFF)
        }
        encoder.encodeString(hex)
    }

    override fun deserialize(decoder: Decoder): Color {
        val value = decoder.decodeString()
        val hex = value.trim()
            .removePrefix("#")
            .removePrefix("0x")
            .removePrefix("0X")

        return when (hex.length) {
            6 -> {
                val red = hex.substring(0, 2).toColorComponent()
                val green = hex.substring(2, 4).toColorComponent()
                val blue = hex.substring(4, 6).toColorComponent()
                Color(red, green, blue)
            }
            8 -> {
                val alpha = hex.substring(0, 2).toColorComponent()
                val red = hex.substring(2, 4).toColorComponent()
                val green = hex.substring(4, 6).toColorComponent()
                val blue = hex.substring(6, 8).toColorComponent()
                Color(red, green, blue, alpha)
            }
            else -> throw SerializationException("Invalid hex color: $value")
        }
    }

    private fun String.toColorComponent(): Int {
        return toIntOrNull(16) ?: throw SerializationException("Invalid hex color component: $this")
    }
}
