package dev.nohus.rift.charactersettings.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import dev.nohus.rift.charactersettings.io.ReadCharacterSettingsUseCase.NeocomButton
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.arrow_down_16px
import dev.nohus.rift.generated.resources.arrow_up_16px
import dev.nohus.rift.generated.resources.delete
import dev.nohus.rift.generated.resources.deleteicon
import dev.nohus.rift.utils.HsbColor
import dev.nohus.rift.utils.toColor

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun NeocomButtons(
    buttons: List<NeocomButton>,
    parentId: String?,
    depth: Int = 0,
    onButtonColorChanged: (String, Int?) -> Unit,
    onButtonsReordered: (String?, Int, Int) -> Unit,
    onButtonRemoved: (String) -> Unit,
) {
    Column {
        buttons.forEachIndexed { index, button ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                modifier = Modifier.hoverBackground().padding(start = Spacing.large * depth),
            ) {
                val colorFilter = button.colorId?.let { ColorFilter.tint(getNeocomIconColor(it)) }
                EveResourceIcon(
                    resourcePath = button.iconPath,
                    colorFilter = colorFilter,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = button.label,
                    style = RiftTheme.typography.bodyPrimary,
                    modifier = Modifier.weight(1f),
                )
                RiftTooltipArea("Move button up") {
                    RiftImageButton(
                        resource = Res.drawable.arrow_up_16px,
                        size = 16.dp,
                        isEnabled = index > 0,
                        onClick = { onButtonsReordered(parentId, index, index - 1) },
                    )
                }
                RiftTooltipArea("Move button down") {
                    RiftImageButton(
                        resource = Res.drawable.arrow_down_16px,
                        size = 16.dp,
                        isEnabled = index < buttons.lastIndex,
                        onClick = { onButtonsReordered(parentId, index, index + 1) },
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.verySmall),
                ) {
                    (1..12).forEach { colorId ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, RiftTheme.colors.borderGreyLight)
                                .background(getNeocomIconColor(colorId))
                                .size(16.dp)
                                .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                                .onClick { onButtonColorChanged(button.id, colorId) },
                        )
                    }
                    RiftTooltipArea(text = "Clear color") {
                        RiftImageButton(
                            resource = Res.drawable.deleteicon,
                            size = 20.dp,
                            onClick = { onButtonColorChanged(button.id, null) },
                        )
                    }
                    RiftTooltipArea("Remove button") {
                        RiftImageButton(
                            resource = Res.drawable.delete,
                            size = 20.dp,
                            onClick = { onButtonRemoved(button.id) },
                        )
                    }
                }
            }
            if (button.children.isNotEmpty()) {
                NeocomButtons(
                    buttons = button.children,
                    parentId = button.id,
                    depth = depth + 1,
                    onButtonColorChanged = onButtonColorChanged,
                    onButtonsReordered = onButtonsReordered,
                    onButtonRemoved = onButtonRemoved,
                )
            }
        }
    }
}

private fun getNeocomIconColor(colorId: Int) = HsbColor(
    hue = (colorId - 1).toFloat() / 12,
    saturation = 0.75f,
    brightness = 176f / 255f,
).toColor()
