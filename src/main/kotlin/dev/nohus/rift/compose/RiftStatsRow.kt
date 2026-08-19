package dev.nohus.rift.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing

@Composable
fun RiftStatsRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .wrapContentWidth(align = Alignment.Start, unbounded = true),
    ) {
        content()
    }
}

@Composable
fun RiftStatsRowItem(
    value: String,
    text: String,
    suffix: String? = null,
    color: Color,
    valueStyle: TextStyle = RiftTheme.typography.displayHighlighted,
    tooltip: AnnotatedString? = null,
) {
    val activeWindowTransition = updateTransition(LocalWindowInfo.current.isWindowFocused)
    val colorWindowTransitionSpec = getActiveWindowTransitionSpec<Color>()
    val color by activeWindowTransition.animateColor(colorWindowTransitionSpec) {
        if (it) color else RiftTheme.colors.textPrimary
    }
    val pointerInteractionStateHolder = remember { PointerInteractionStateHolder() }
    RiftTooltipArea(
        text = tooltip,
    ) {
        Row(
            modifier = Modifier
                .pointerInteraction(pointerInteractionStateHolder)
                .padding(end = Spacing.large)
                .height(IntrinsicSize.Max),
        ) {
            RiftVerticalGlowLine(pointerInteractionStateHolder, color, Side.Right)
            Spacer(Modifier.width(Spacing.large))
            Column {
                AnimatedContent(value) { value ->
                    Text(
                        text = buildAnnotatedString {
                            append(value)
                            if (suffix != null) {
                                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                                    append(suffix)
                                }
                            }
                        },
                        style = valueStyle,
                    )
                }
                Text(
                    text = text,
                    style = RiftTheme.typography.bodySecondary,
                )
            }
        }
    }
}
