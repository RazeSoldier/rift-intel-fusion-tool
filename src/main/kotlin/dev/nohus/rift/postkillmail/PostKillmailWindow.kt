package dev.nohus.rift.postkillmail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.TitleBarStyle
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_clipboard
import dev.nohus.rift.generated.resources.window_killreport
import dev.nohus.rift.postkillmail.PostKillmailViewModel.UiState
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun PostKillmailWindow(
    inputModel: PostKillmailInputModel,
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: PostKillmailViewModel = viewModel(inputModel)
    val state by viewModel.state.collectAsState()

    RiftWindow(
        title = "Post killmail",
        icon = Res.drawable.window_killreport,
        state = windowState,
        onCloseClick = onCloseRequest,
        titleBarStyle = TitleBarStyle.Small,
        withContentPadding = false,
        isResizable = false,
    ) {
        WindowDraggableArea {
            PostKillmailContent(
                state = state,
                viewModel = viewModel,
                onCloseRequest = onCloseRequest,
            )
        }
    }
}

@Composable
private fun PostKillmailContent(
    state: UiState,
    viewModel: PostKillmailViewModel,
    onCloseRequest: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
        modifier = Modifier.padding(Spacing.medium),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Post the kill you copied to zKillboard?",
                    style = RiftTheme.typography.bodyPrimary,
                )
                Text(
                    text = "You can control this feature in settings.",
                    style = RiftTheme.typography.detailSecondary,
                )
            }
            RiftDropdownWithLabel(
                label = "Delay:",
                items = KillmailPostingDelay.entries,
                selectedItem = state.selectedDelay,
                onItemSelected = viewModel::onDelaySelected,
                getItemName = { it.displayName },
                maxItems = 2,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.weight(1f))
            RiftButton(
                text = "Cancel",
                type = ButtonType.Secondary,
                cornerCut = ButtonCornerCut.None,
                onClick = onCloseRequest,
            )
            RiftButton(
                text = if (state.isPosting) "Posting…" else "Post killmail",
                onClick = viewModel::onPostClick,
                isEnabled = !state.isPosting,
            )
        }
    }
}
