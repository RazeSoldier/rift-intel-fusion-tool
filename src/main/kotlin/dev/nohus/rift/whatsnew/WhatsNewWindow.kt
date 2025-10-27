package dev.nohus.rift.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import dev.nohus.rift.compose.AffiliateCode
import dev.nohus.rift.compose.Bullet
import dev.nohus.rift.compose.CreatorCode
import dev.nohus.rift.compose.Patrons
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.window_redeem
import dev.nohus.rift.viewModel
import dev.nohus.rift.whatsnew.WhatsNewViewModel.UiState
import dev.nohus.rift.whatsnew.WhatsNewViewModel.Version
import dev.nohus.rift.windowing.WindowManager.RiftWindowState

@Composable
fun WhatsNewWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: WhatsNewViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "What's New",
        icon = Res.drawable.window_redeem,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        WhatsNewWindowContent(
            state = state,
        )
    }
}

@Composable
private fun WhatsNewWindowContent(
    state: UiState,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        ScrollbarLazyColumn(
            modifier = Modifier.weight(1f),
        ) {
            items(state.versions) {
                VersionItem(it)
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Box(Modifier.weight(1f)) {
                    AffiliateCode()
                }
                Box(Modifier.weight(1f)) {
                    CreatorCode()
                }
            }
            Patrons(state.patrons, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun VersionItem(version: Version) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = Spacing.medium),
        ) {
            Divider(
                color = RiftTheme.colors.divider,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Version ${version.version}",
                style = RiftTheme.typography.headlineHighlighted.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .padding(horizontal = Spacing.medium),
            )
            Divider(
                color = RiftTheme.colors.divider,
                modifier = Modifier.weight(1f),
            )
        }
        version.points.forEach { point ->
            Row {
                if (!point.isHighlighted) {
                    Bullet()
                    Spacer(Modifier.width(Spacing.medium))
                }
                val style = if (point.isHighlighted) {
                    RiftTheme.typography.headerPrimary.copy(
                        color = RiftTheme.colors.textSpecialHighlighted,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    RiftTheme.typography.headerPrimary
                }
                Text(
                    text = point.text,
                    style = style,
                )
            }
        }
    }
}
