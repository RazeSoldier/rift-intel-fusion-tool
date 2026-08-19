package dev.nohus.rift.compose

import androidx.compose.animation.core.Spring.StiffnessVeryLow
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import dev.nohus.rift.compose.theme.RiftTheme

@Composable
fun RiftProgressBar(
    percentage: Float,
    height: Dp,
    secondaryPercentage: Float? = null,
    tertiaryPercentage: Float? = null,
    color: Color,
    secondaryColor: Color? = null,
    tertiaryColor: Color? = null,
    hasInitialAnimation: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var width by remember { mutableStateOf(0.dp) }
    Box(
        modifier = modifier
            .height(height)
            .onGloballyPositioned { coordinates ->
                width = with(density) { coordinates.size.width.toDp() }
            }
            .background(RiftTheme.colors.progressBarBackground),
    ) {
        var animatedPercentage by remember { mutableStateOf(if (hasInitialAnimation) 0f else percentage) }
        var animatedSecondaryPercentage by remember { mutableStateOf(if (hasInitialAnimation) 0f else secondaryPercentage ?: 0f) }
        var animatedTertiaryPercentage by remember { mutableStateOf(if (hasInitialAnimation) 0f else tertiaryPercentage ?: 0f) }
        val progressWidth by animateDpAsState(animatedPercentage * width, spring(stiffness = StiffnessVeryLow))
        val secondaryProgressWidth by animateDpAsState(animatedSecondaryPercentage * width, spring(stiffness = StiffnessVeryLow))
        val tertiaryProgressWidth by animateDpAsState(animatedTertiaryPercentage * width, spring(stiffness = StiffnessVeryLow))
        LaunchedEffect(percentage, secondaryPercentage) {
            animatedPercentage = percentage
            animatedSecondaryPercentage = secondaryPercentage ?: 0f
            animatedTertiaryPercentage = tertiaryPercentage ?: 0f
        }
        ProgressBarFill(progressWidth, height, color)
        if (secondaryColor != null) {
            ProgressBarFill(secondaryProgressWidth, height, secondaryColor, Modifier.offset(x = progressWidth))
        }
        if (tertiaryColor != null) {
            ProgressBarFill(tertiaryProgressWidth, height, tertiaryColor, Modifier.offset(x = progressWidth + secondaryProgressWidth))
        }
    }
}

@Composable
private fun ProgressBarFill(
    width: Dp,
    height: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Box(
            modifier = Modifier
                .size(width, height)
                .graphicsLayer(renderEffect = BlurEffect(6f, 6f, edgeTreatment = TileMode.Decal))
                .background(color),
        ) {}
        Box(
            modifier = Modifier
                .size(width, height)
                .background(color),
        ) {}
    }
}
