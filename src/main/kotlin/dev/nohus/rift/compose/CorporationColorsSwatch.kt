package dev.nohus.rift.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.CorporationColorsSwatch(
    colors: CorporationColors,
    backgroundWidth: Dp?,
    size: Dp,
    pointerInteractionStateHolder: PointerInteractionStateHolder,
) {
    val mainColor by animateColorAsState(colors.main)
    val secondaryColor by animateColorAsState(colors.secondary ?: Color.Transparent)
    val tertiaryColor by animateColorAsState(colors.tertiary ?: Color.Transparent)
    val isActive = pointerInteractionStateHolder.isHovered
    val alpha by animateFloatAsState(if (isActive) 0.5f else 0.1f)
    val blur by animateFloatAsState(if (isActive) 4f else 0.5f)
    val extent by animateFloatAsState(if (isActive) 4f else 2f)
    val cornerCut = 15.dp
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .width(backgroundWidth ?: size)
            .fillMaxHeight()
            .clip(CutCornerShape(bottomEnd = cornerCut))
            .modifyIfNotNull(backgroundWidth) {
                background(Color.Black)
            }
            .modifyIfNotNull(backgroundWidth) {
                background(mainColor.copy(alpha = 0.2f))
            },
    ) {
        val size = if (colors.secondary != null && colors.tertiary != null) size else 64.dp * 0.75f
        // Main color and blur
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                .clip(CutCornerShape(topStartPercent = 100))
                .size(size + extent.dp)
                .background(mainColor.copy(alpha = alpha)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .clip(CutCornerShape(topStartPercent = 100))
                .clip(CutCornerShape(bottomEnd = cornerCut + 2.dp))
                .size(size)
                .background(mainColor),
        )

        if (colors.secondary != null && colors.tertiary != null) {
            // 3 colors swatch, show the secondary and tertiary color
            val size = 64.dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CutCornerShape(topStartPercent = 100))
                    .size((size - cornerCut) * 0.66f + cornerCut)
                    .background(secondaryColor),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CutCornerShape(topStartPercent = 100))
                    .size((size - cornerCut) * 0.33f + cornerCut)
                    .background(tertiaryColor),
            )
        } else if (colors.secondary != null) {
            // 2 colors swatch, show the secondary color
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CutCornerShape(topStartPercent = 100))
                    .size((size - cornerCut) * 0.5f + cornerCut)
                    .background(secondaryColor),
            )
        }
    }
}
