package dev.nohus.rift.compose

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.contact_tag
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun RiftSideNavigation(
    footer: @Composable () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val activeWindowTransition = updateTransition(LocalWindowInfo.current.isWindowFocused)
    val colorWindowTransitionSpec = getActiveWindowTransitionSpec<Color>()
    val color by activeWindowTransition.animateColor(colorWindowTransitionSpec) {
        if (it) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.025f)
    }

    Column(
        modifier = Modifier
            .background(color)
            .width(240.dp)
            .fillMaxHeight(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(start = 8.dp),
            content = content,
        )
        Spacer(Modifier.weight(1f))
        footer()
    }
}

@Composable
fun LazyItemScope.RiftSideNavigationHeader(text: String) {
    Text(
        text = text,
        style = RiftTheme.typography.detailSecondary,
        modifier = Modifier
            .animateItem()
            .padding(top = 16.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
    )
}

@Composable
fun LazyItemScope.RiftSideNavigationItem(
    text: String,
    count: Int?,
    icon: DrawableResource?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RiftSideNavigationItem(
        text = text,
        count = count,
        icon = { color ->
            if (icon != null) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(color),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp),
                )
            }
        },
        isSelected = isSelected,
        onClick = onClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.RiftSideNavigationItem(
    text: String,
    count: Int?,
    icon: @Composable (color: Color) -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEnabled = count == null || count > 0
    val pointerInteractionStateHolder = rememberPointerInteractionStateHolder()
    Box(
        modifier = modifier
            .animateItem()
            .onClick { onClick() }
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .pointerInteraction(pointerInteractionStateHolder)
            .height(IntrinsicSize.Min)
            .hoverBackground(
                pressColor = RiftTheme.colors.backgroundPrimary,
                pointerInteractionStateHolder = pointerInteractionStateHolder,
                isSelected = isSelected,
            ),
    ) {
        val activeWindowTransition = updateTransition(LocalWindowInfo.current.isWindowFocused)
        val colorWindowTransitionSpec = getActiveWindowTransitionSpec<Color>()
        val glowLineColor by activeWindowTransition.animateColor(colorWindowTransitionSpec) {
            if (it) RiftTheme.colors.borderPrimaryLight else RiftTheme.colors.textPrimary
        }
        if (isSelected) {
            RiftVerticalGlowLine(pointerInteractionStateHolder, glowLineColor, Side.Left, isSelected = LocalWindowInfo.current.isWindowFocused)
        }

        val color = when {
            isSelected && isEnabled -> RiftTheme.colors.textHighlighted
            isEnabled -> RiftTheme.colors.textPrimary
            else -> RiftTheme.colors.textDisabled
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(top = 4.dp, bottom = 4.dp, start = 8.dp, end = 16.dp),
        ) {
            icon(color)
            Text(
                text = text,
                style = RiftTheme.typography.bodyPrimary.copy(color = color),
                maxLines = 1,
                overflow = TextOverflow.Visible,
                softWrap = false,
                modifier = Modifier
                    .padding(end = Spacing.medium)
                    .fadingRightEdge()
                    .padding(start = 8.dp)
                    .weight(1f),
            )
            if (count != null && isEnabled) {
                Text(
                    text = "$count",
                    style = when {
                        isSelected -> RiftTheme.typography.bodyHighlighted
                        else -> RiftTheme.typography.bodySecondary
                    },
                )
            }
        }
    }
}
