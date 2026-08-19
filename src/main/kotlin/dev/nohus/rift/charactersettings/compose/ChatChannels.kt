package dev.nohus.rift.charactersettings.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.deleteicon
import dev.nohus.rift.generated.resources.window_warning
import dev.nohus.rift.windowing.LocalRiftWindowState

private data class ChatChannel(
    val id: String,
    val name: String,
)

@Composable
fun WindowScope.ChatChannels(
    channels: Map<String, String>,
    onChannelsChanged: (Map<String, String>) -> Unit,
) {
    var workingChannels by remember(channels) {
        mutableStateOf(channels.map { (id, name) -> ChatChannel(id, name) })
    }
    var channelToDelete by remember { mutableStateOf<ChatChannel?>(null) }

    LaunchedEffect(workingChannels) {
        val updatedChannels = workingChannels.associate { it.id to it.name }
        if (updatedChannels != channels) onChannelsChanged(updatedChannels)
    }

    if (workingChannels.isEmpty()) {
        Text(
            text = "No joined chat channels are saved in this character profile.",
            style = RiftTheme.typography.bodySecondary,
        )
    } else {
        ScrollbarLazyColumn {
            items(workingChannels.size) { index ->
                val channel = workingChannels[index]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .hoverBackground()
                        .padding(Spacing.medium),
                ) {
                    RiftImageButton(
                        resource = Res.drawable.deleteicon,
                        size = 20.dp,
                        onClick = { channelToDelete = channel },
                    )
                    Text(
                        text = channel.name,
                        style = RiftTheme.typography.bodyPrimary,
                        modifier = Modifier.width(200.dp),
                    )
                }
            }
        }
    }

    channelToDelete?.let { channel ->
        val parentState = LocalRiftWindowState.current
        if (parentState != null) {
            RiftDialog(
                title = "Leave channel?",
                icon = Res.drawable.window_warning,
                parentState = parentState,
                state = rememberWindowState(width = 360.dp, height = Dp.Unspecified),
                onCloseClick = { channelToDelete = null },
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.large),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Are you sure you want to leave “${channel.name}”?",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        RiftButton(
                            text = "Cancel",
                            type = ButtonType.Secondary,
                            cornerCut = ButtonCornerCut.BottomLeft,
                            onClick = { channelToDelete = null },
                            modifier = Modifier.weight(1f),
                        )
                        RiftButton(
                            text = "Leave",
                            type = ButtonType.Negative,
                            onClick = {
                                workingChannels = workingChannels - channel
                                channelToDelete = null
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
