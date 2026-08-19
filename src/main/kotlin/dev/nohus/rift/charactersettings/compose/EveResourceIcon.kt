package dev.nohus.rift.charactersettings.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.nohus.rift.charactersettings.EveResourceResolver
import dev.nohus.rift.di.koin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import org.jetbrains.skia.Image as SkiaImage

/**
 * Shows an icon from an EVE resource path:
 * @param resourcePath - res:/ui/Texture/WindowIcons/evemail.png
 */
@Composable
fun EveResourceIcon(
    resourcePath: String,
    colorFilter: ColorFilter? = null,
    modifier: Modifier = Modifier,
) {
    val eveResourceResolver: EveResourceResolver = remember { koin.get() }
    val image by produceState<ImageBitmap?>(initialValue = null, resourcePath) {
        value = withContext(Dispatchers.IO) {
            eveResourceResolver(resourcePath)?.takeIf { it.exists() }?.let { path ->
                runCatching { SkiaImage.makeFromEncoded(path.readBytes()).toComposeImageBitmap() }.getOrNull()
            }
        }
    }
    image?.let { image ->
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = colorFilter,
            modifier = modifier,
        )
    } ?: run {
        Box(modifier)
    }
}
