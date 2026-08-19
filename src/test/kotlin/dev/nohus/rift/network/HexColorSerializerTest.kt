package dev.nohus.rift.network

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class HexColorSerializerTest : FreeSpec({

    "Serializes opaque colors as RGB hex" {
        Json.encodeToString(HexColorSerializer, Color(0xFF58A7BF)) shouldBe "\"#58A7BF\""
    }

    "Serializes transparent colors as ARGB hex" {
        Json.encodeToString(HexColorSerializer, Color(0x8058A7BF)) shouldBe "\"#8058A7BF\""
    }

    "Deserializes RGB hex colors" {
        Json.decodeFromString(HexColorSerializer, "\"#58A7BF\"").toArgb() shouldBe Color(0xFF58A7BF).toArgb()
    }

    "Deserializes ARGB hex colors" {
        Json.decodeFromString(HexColorSerializer, "\"#8058A7BF\"").toArgb() shouldBe Color(0x8058A7BF).toArgb()
    }

    "Rejects invalid hex colors" {
        shouldThrow<SerializationException> {
            Json.decodeFromString(HexColorSerializer, "\"#58A7BG\"")
        }
    }
})
