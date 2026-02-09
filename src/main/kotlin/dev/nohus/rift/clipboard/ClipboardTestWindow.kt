package dev.nohus.rift.clipboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nohus.rift.clipboard.ClipboardTestViewModel.ClipboardImportType
import dev.nohus.rift.clipboard.ClipboardTestViewModel.UiState
import dev.nohus.rift.compose.MulticolorIconType
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftMulticolorIcon
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftToggleButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWarningBanner
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.ToggleButtonType
import dev.nohus.rift.compose.fadingRightEdge
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.*
import dev.nohus.rift.settings.JumpBridgesParser
import dev.nohus.rift.settings.SovereigntyUpgradesParser
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.stringResource

@Composable
fun ClipboardTestWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: ClipboardTestViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = stringResource(Res.string.clipboard_test_window_title),
        icon = Res.drawable.window_clipboard,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        ClipboardTestWindowContent(
            state = state,
            viewModel = viewModel,
        )
    }
}

@Composable
private fun ClipboardTestWindowContent(
    state: UiState,
    viewModel: ClipboardTestViewModel,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            RiftToggleButton(
                text = stringResource(Res.string.clipboard_test_window_jump_bridge),
                isSelected = state.type == ClipboardImportType.JumpBridges,
                type = ToggleButtonType.Left,
                onClick = { viewModel.onImportTypeChange(ClipboardImportType.JumpBridges) },
            )
            RiftToggleButton(
                text = stringResource(Res.string.clipboard_test_window_sov_upgrade),
                isSelected = state.type == ClipboardImportType.SovereigntyUpgrades,
                type = ToggleButtonType.Right,
                onClick = { viewModel.onImportTypeChange(ClipboardImportType.SovereigntyUpgrades) },
            )
        }

        ScrollbarColumn(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, RiftTheme.colors.borderGrey)
                .padding(Spacing.small),
            scrollbarModifier = Modifier.padding(vertical = Spacing.small),
            contentPadding = PaddingValues(vertical = Spacing.verySmall),
        ) {
            when (state.type) {
                ClipboardImportType.JumpBridges -> {
                    JumpBridgesContent(state)
                }
                ClipboardImportType.SovereigntyUpgrades -> {
                    SovereigntyUpgradesContent(state)
                }
                null -> {
                    EmptyState(stringResource(Res.string.clipboard_test_window_empty_state))
                }
            }
        }
    }
}

@Composable
private fun JumpBridgesContent(state: UiState) {
    when (val result = state.jumpBridgesResult) {
        JumpBridgesParser.ParsingResult.Empty, null -> {
            EmptyState(stringResource(Res.string.clipboard_test_window_jump_bridge_empty))
        }

        JumpBridgesParser.ParsingResult.TooShort -> {
            WarningState(stringResource(Res.string.clipboard_test_window_jump_bridge_too_short))
        }

        is JumpBridgesParser.ParsingResult.ParsedNotEnough -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
            ) {
                WarningState(stringResource(Res.string.clipboard_test_window_jump_bridge_not_enough))
                result.lines.forEach { line ->
                    JumpBridgeParsedLine(line)
                }
            }
        }

        is JumpBridgesParser.ParsingResult.ParsedValid -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
            ) {
                SuccessState(stringResource(Res.string.clipboard_test_window_jump_bridge_valid, result.connections.size))
                result.lines.forEach { line ->
                    JumpBridgeParsedLine(line)
                }
            }
        }
    }
}

@Composable
private fun JumpBridgeParsedLine(line: JumpBridgesParser.ParsedLine) {
    when (line) {
        is JumpBridgesParser.ParsedLine.Connection -> {
            ParsedLine(
                icon = MulticolorIconType.Check,
                description = stringResource(Res.string.clipboard_test_window_jump_bridge_line_valid, line.connection.from.name, line.connection.to.name),
                line = line.text,
            )
        }

        is JumpBridgesParser.ParsedLine.NoSystems -> {
            ParsedLine(
                icon = MulticolorIconType.Info,
                description = stringResource(Res.string.clipboard_test_window_jump_bridge_line_no_system),
                line = line.text,
            )
        }

        is JumpBridgesParser.ParsedLine.OneSystem -> {
            ParsedLine(
                icon = MulticolorIconType.Warning,
                description = stringResource(Res.string.clipboard_test_window_jump_bridge_line_one_system, line.system.name),
                line = line.text,
            )
        }

        is JumpBridgesParser.ParsedLine.TooManySystems -> {
            ParsedLine(
                icon = MulticolorIconType.Warning,
                description = stringResource(Res.string.clipboard_test_window_jump_bridge_line_too_many_system, line.systems.joinToString { it.name }),
                line = line.text,
            )
        }
    }
}

@Composable
private fun SovereigntyUpgradesContent(state: UiState) {
    when (val result = state.sovereigntyUpgradesResult) {
        SovereigntyUpgradesParser.ParsingResult.Empty, null -> {
            EmptyState(stringResource(Res.string.clipboard_test_window_sov_upgrade_empty))
        }
        is SovereigntyUpgradesParser.ParsingResult.ParsedNotEnough -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
            ) {
                WarningState(stringResource(Res.string.clipboard_test_window_sov_upgrade_no_enough))
                result.lines.forEach { line ->
                    SovereigntyUpgradesParsedLine(line)
                }
            }
        }
        is SovereigntyUpgradesParser.ParsingResult.ParsedValid -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
            ) {
                SuccessState(stringResource(Res.string.clipboard_test_window_sov_upgrade_valid, result.upgrades.size))
                result.lines.forEach { line ->
                    SovereigntyUpgradesParsedLine(line)
                }
            }
        }
    }
}

@Composable
private fun SovereigntyUpgradesParsedLine(line: SovereigntyUpgradesParser.ParsedLine) {
    when (line) {
        is SovereigntyUpgradesParser.ParsedLine.SystemWithUpgrades -> {
            ParsedLine(
                icon = MulticolorIconType.Check,
                description = stringResource(Res.string.clipboard_test_window_sov_upgrade_line_valid, line.system.name, line.upgrades.joinToString { it.name }),
                line = line.text,
            )
        }
        is SovereigntyUpgradesParser.ParsedLine.NoSystem -> {
            ParsedLine(
                icon = MulticolorIconType.Info,
                description = stringResource(Res.string.clipboard_test_window_sov_upgrade_line_no_system),
                line = line.text,
            )
        }
        is SovereigntyUpgradesParser.ParsedLine.NoUpgrades -> {
            ParsedLine(
                icon = MulticolorIconType.Warning,
                description = stringResource(Res.string.clipboard_test_window_sov_upgrade_line_no_upgrade, line.system.name),
                line = line.text,
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Text(
        text = message,
        style = RiftTheme.typography.headlineSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}

@Composable
private fun WarningState(message: String) {
    Text(
        text = message,
        style = RiftTheme.typography.headlinePrimary.copy(color = RiftTheme.colors.warningColor),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}

@Composable
private fun SuccessState(message: String) {
    Text(
        text = message,
        style = RiftTheme.typography.headlinePrimary.copy(color = RiftTheme.colors.textHighlighted),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}

@Composable
private fun ParsedLine(
    icon: MulticolorIconType,
    description: String,
    line: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier
            .height(24.dp)
            .fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier
                .border(1.dp, RiftTheme.colors.borderGrey)
                .background(RiftTheme.colors.windowBackgroundSecondary)
                .padding(Spacing.small),
        ) {
            RiftTooltipArea(
                text = description,
            ) {
                RiftMulticolorIcon(type = icon)
            }
        }
        Text(
            text = line,
            style = RiftTheme.typography.bodyPrimary,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false,
            modifier = Modifier
                .weight(1f)
                .fadingRightEdge(),
        )
    }
}
