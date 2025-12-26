package dev.nohus.rift.wizard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AnimatedImage
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftMessageDialog
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.TypingText
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.configurationpack.displayName
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.*
import dev.nohus.rift.get
import dev.nohus.rift.settings.persistence.ConfigurationPack
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import dev.nohus.rift.wizard.WizardViewModel.EveInstallationState
import dev.nohus.rift.wizard.WizardViewModel.UiState
import dev.nohus.rift.wizard.WizardViewModel.WizardStep
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WizardWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: WizardViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "RIFT Intel Fusion Tool",
        icon = Res.drawable.window_agent,
        state = windowState,
        onCloseClick = onCloseRequest,
        isResizable = false,
    ) {
        WizardWindowContent(
            state = state,
            onSetEveInstallationClick = viewModel::onSetEveInstallationClick,
            onCharactersClick = viewModel::onCharactersClick,
            onConfigurationPackChange = viewModel::onConfigurationPackChange,
            onSetIntelChannelsClick = viewModel::onSetIntelChannelsClick,
            onContinueClick = viewModel::onContinueClick,
            onKeyEvent = viewModel::onKeyEvent,
        )

        state.dialogMessage?.let {
            RiftMessageDialog(
                dialog = it,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseDialogMessage,
            )
        }

        if (state.onFinishedEvent.get()) onCloseRequest()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WizardWindowContent(
    state: UiState,
    onSetEveInstallationClick: () -> Unit,
    onCharactersClick: () -> Unit,
    onConfigurationPackChange: (ConfigurationPack?) -> Unit,
    onSetIntelChannelsClick: () -> Unit,
    onContinueClick: () -> Unit,
    onKeyEvent: (KeyEvent) -> Unit,
) {
    val focusRequester = FocusRequester()
    Row(
        modifier = Modifier
            .onKeyEvent {
                onKeyEvent(it)
                false
            }
            .focusRequester(focusRequester)
            .focusable()
            .onClick { focusRequester.requestFocus() },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(end = Spacing.large),
        ) {
            AnimatedImage(
                resource = "aura.gif",
                modifier = Modifier
                    .size(200.dp)
                    .border(2.dp, RiftTheme.colors.borderPrimary),
            )
            Image(
                painter = painterResource(Res.drawable.partner_400),
                contentDescription = null,
                modifier = Modifier
                    .width(200.dp),
            )
        }
        when (val step = state.step) {
            WizardStep.Welcome -> WelcomeStep(
                onContinueClick = onContinueClick,
            )
            is WizardStep.EveInstallation -> EveInstallationStep(
                step = step,
                onSetEveInstallationClick = onSetEveInstallationClick,
                onContinueClick = onContinueClick,
            )
            is WizardStep.Characters -> CharactersStep(
                step = step,
                onCharactersClick = onCharactersClick,
                onContinueClick = onContinueClick,
            )
            is WizardStep.ConfigurationPacks -> ConfigurationPacksStep(
                step = step,
                onConfigurationPackChange = onConfigurationPackChange,
                onContinueClick = onContinueClick,
            )
            is WizardStep.IntelChannels -> IntelChannelsStep(
                step = step,
                onSetIntelChannelsClick = onSetIntelChannelsClick,
                onContinueClick = onContinueClick,
            )
            WizardStep.Finish -> FinishStep(
                onContinueClick = onContinueClick,
            )
        }
    }
}

@Composable
private fun WelcomeStep(
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
    ) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                append(stringResource(Res.string.wizard_window_welcome_title))
            }
            append(stringResource(Res.string.wizard_window_welcome_content),)
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
    }
}

@Composable
private fun EveInstallationStep(
    step: WizardStep.EveInstallation,
    onSetEveInstallationClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    val isContinueWarning = when (step.state) {
        EveInstallationState.None -> true
        EveInstallationState.Detected -> false
        EveInstallationState.Set -> false
    }
    var hasFinishedTyping by remember { mutableStateOf(false) }

    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
        isWarning = isContinueWarning,
    ) {
        val text = buildAnnotatedString {
            when (step.state) {
                EveInstallationState.None -> {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append(stringResource(Res.string.wizard_window_eve_not_detected))
                    }
                    append(stringResource(Res.string.wizard_window_eve_not_detected_content),)
                }
                EveInstallationState.Detected -> {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append(stringResource(Res.string.wizard_window_eve_detected))
                    }
                    append(stringResource(Res.string.wizard_window_eve_detected_content))
                }
                EveInstallationState.Set -> {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append(stringResource(Res.string.wizard_window_eve_located))
                    }
                    append(stringResource(Res.string.wizard_window_eve_located_content))
                }
            }
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large)
                .align(Alignment.CenterHorizontally),
        ) {
            val (type, buttonText) = when (step.state) {
                EveInstallationState.None -> ButtonType.Primary to stringResource(Res.string.wizard_window_select_installation_button)
                EveInstallationState.Detected -> ButtonType.Secondary to stringResource(Res.string.wizard_window_check_installation_button)
                EveInstallationState.Set -> ButtonType.Secondary to stringResource(Res.string.wizard_window_change_installation_button)
            }
            RiftButton(
                text = buttonText,
                type = type,
                cornerCut = ButtonCornerCut.Both,
                onClick = onSetEveInstallationClick,
            )
        }
    }
}

@Composable
private fun CharactersStep(
    step: WizardStep.Characters,
    onCharactersClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
        isWarning = step.authenticatedCharacterCount < step.characterCount,
    ) {
        val text = buildAnnotatedString {
            if (step.characterCount == 0) {
                withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                    append(stringResource(Res.string.wizard_window_no_characters_detected))
                }
                append(stringResource(Res.string.wizard_window_no_characters_detected_content))
            } else {
                if (step.authenticatedCharacterCount == 0) {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append(stringResource(Res.string.wizard_window_characters_detected))
                    }
                    append(stringResource(Res.string.wizard_window_characters_detected_content, step.characterCount, step.characterCount))
                } else if (step.authenticatedCharacterCount < step.characterCount) {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append(stringResource(Res.string.wizard_window_characters_partially_setup))
                    }
                    append(stringResource(Res.string.wizard_window_characters_partially_setup_content, step.characterCount, step.authenticatedCharacterCount))
                } else {
                    withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                        append(stringResource(Res.string.wizard_window_characters_setup))
                    }
                    append(stringResource(Res.string.wizard_window_characters_setup_content))
                }
            }
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large)
                .align(Alignment.CenterHorizontally),
        ) {
            val (type, buttonText) = if (step.authenticatedCharacterCount < step.characterCount) {
                ButtonType.Primary to stringResource(Res.string.wizard_window_authenticate_characters_button)
            } else {
                ButtonType.Secondary to stringResource(Res.string.wizard_window_check_characters_button)
            }
            RiftButton(
                text = buttonText,
                type = type,
                cornerCut = ButtonCornerCut.Both,
                onClick = onCharactersClick,
            )
        }
    }
}

@Composable
private fun ConfigurationPacksStep(
    step: WizardStep.ConfigurationPacks,
    onConfigurationPackChange: (ConfigurationPack?) -> Unit,
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    val getPackName: (ConfigurationPack?) -> String = { it.displayName }
    StepContent(
        onContinueClick = onContinueClick,
    ) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                append(stringResource(Res.string.wizard_window_alliance_features))
            }
            if (step.pack != null) {
                append(stringResource(Res.string.wizard_window_enable_pack_question, getPackName(step.pack)))
            } else {
                append(stringResource(Res.string.wizard_window_enable_pack_default))
            }
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large)
                .align(Alignment.CenterHorizontally),
        ) {
            RiftDropdownWithLabel(
                label = stringResource(Res.string.wizard_window_configuration_pack),
                items = listOf(null) + ConfigurationPack.entries,
                selectedItem = step.pack,
                onItemSelected = onConfigurationPackChange,
                getItemName = getPackName,
            )
        }
    }
}

@Composable
private fun IntelChannelsStep(
    step: WizardStep.IntelChannels,
    onSetIntelChannelsClick: () -> Unit,
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
        isWarning = !step.hasChannels,
        warningButtonText = stringResource(Res.string.skip),
    ) {
        val text = buildAnnotatedString {
            if (step.hasChannels) {
                withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                    append(stringResource(Res.string.wizard_window_intel_channels_setup))
                }
                append(stringResource(Res.string.wizard_window_intel_channels_setup_content))
            } else {
                withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                    append(stringResource(Res.string.wizard_window_intel_chanels_not_setup))
                }
                append(stringResource(Res.string.wizard_window_intel_chanels_not_setup_content))
            }
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large)
                .align(Alignment.CenterHorizontally),
        ) {
            val (type, buttonText) = if (!step.hasChannels) {
                ButtonType.Primary to stringResource(Res.string.wizard_window_add_intel_channel_button)
            } else {
                ButtonType.Secondary to stringResource(Res.string.wizard_window_change_intel_chanel)
            }
            RiftButton(
                text = buttonText,
                type = type,
                cornerCut = ButtonCornerCut.Both,
                onClick = onSetIntelChannelsClick,
            )
        }
    }
}

@Composable
private fun FinishStep(
    onContinueClick: () -> Unit,
) {
    var hasFinishedTyping by remember { mutableStateOf(false) }
    StepContent(
        onContinueClick = onContinueClick,
        isContinueVisible = hasFinishedTyping,
    ) {
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = RiftTheme.colors.textHighlighted, fontSize = RiftTheme.typography.headlineHighlighted.fontSize)) {
                append(stringResource(Res.string.wizard_window_all_done))
            }
            append(stringResource(Res.string.wizard_window_all_done_content))
        }
        TypingText(
            text = text,
            style = RiftTheme.typography.headerPrimary,
            onFinishedTyping = { hasFinishedTyping = true },
        )
        AnimatedVisibility(
            visible = hasFinishedTyping,
            enter = fadeIn(),
            modifier = Modifier
                .padding(top = Spacing.large),
        ) {
            Image(
                painter = painterResource(Res.drawable.tray_tray_64),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun StepContent(
    onContinueClick: () -> Unit,
    isContinueVisible: Boolean = true,
    isWarning: Boolean = false,
    warningButtonText: String = stringResource(Res.string.wizard_window_continue_anyway),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        content()
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(
            visible = isContinueVisible,
            enter = fadeIn(),
            modifier = Modifier.align(Alignment.End),
        ) {
            val text = if (isWarning) warningButtonText else stringResource(Res.string.`continue`)
            val type = if (isWarning) ButtonType.Negative else ButtonType.Primary
            RiftButton(
                text = text,
                type = type,
                onClick = onContinueClick,
            )
        }
    }
}
