package dev.nohus.rift.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nohus.rift.generated.resources.Res
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun AnimatedImage(
    resource: String,
    modifier: Modifier = Modifier,
) {
    KamelImage(
        resource = {
            asyncPainterResource(Res.getUri(resource))
        },
        contentDescription = null,
        modifier = modifier,
    )
}
