package dev.nohus.rift.pings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.clipboard.Clipboard
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftOpportunityCard
import dev.nohus.rift.compose.RiftOpportunityCardBottomContent
import dev.nohus.rift.compose.RiftOpportunityCardButton
import dev.nohus.rift.compose.RiftOpportunityCardCategory
import dev.nohus.rift.compose.RiftOpportunityCardType
import dev.nohus.rift.compose.RiftStatsRow
import dev.nohus.rift.compose.RiftStatsRowItem
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.annotateLinks
import dev.nohus.rift.compose.getNow
import dev.nohus.rift.compose.getRelativeTime
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.copy_16px
import dev.nohus.rift.generated.resources.fitting_16px
import dev.nohus.rift.generated.resources.microphone
import dev.nohus.rift.generated.resources.window_question
import dev.nohus.rift.generated.resources.window_sovereignty
import dev.nohus.rift.jabber.GetPapsUseCase.Paps
import dev.nohus.rift.pings.PingsViewModel.UiState
import dev.nohus.rift.utils.formatNumber
import dev.nohus.rift.utils.openBrowser
import dev.nohus.rift.utils.toURIOrNull
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager
import java.time.Duration
import java.time.ZoneId

@Composable
fun PingsWindow(
    windowState: WindowManager.RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: PingsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Pings",
        icon = Res.drawable.window_sovereignty,
        state = windowState,
        tuneContextMenuItems = listOf(
            ContextMenuItem.CheckboxItem(
                text = "Show PAPs",
                isSelected = state.isShowingPaps == true,
                onClick = {
                    if (state.isShowingPaps == true) {
                        viewModel.isShowingPapsChanged(false)
                    } else {
                        viewModel.onPapsDialogOpen()
                    }
                },
            ),
        ),
        onCloseClick = onCloseRequest,
        withContentPadding = false,
    ) {
        PingsWindowContent(
            state = state,
            onChoosePapsClick = { viewModel.onPapsDialogOpen() },
            onCheckPapsClick = { viewModel.onCheckPapsClick() },
            onOpenJabberClick = viewModel::onOpenJabberClick,
            onMumbleClick = viewModel::onMumbleClick,
        )

        if (state.isPapsDialogOpen) {
            RiftDialog(
                title = "View your PAPs",
                icon = Res.drawable.window_question,
                parentState = windowState,
                state = rememberWindowState(width = 380.dp, height = Dp.Unspecified),
                onCloseClick = viewModel::onPapsDialogClose,
            ) {
                PapsDialogContent(viewModel)
            }
        }
    }
}

@Composable
private fun PapsDialogContent(viewModel: PingsViewModel) {
    Column {
        Text(
            text = """
                Your PAPs (fleet participation statistics) can be shown in the Pings window.
                
                It's recommended to only turn this on if you only use Jabber in RIFT, as it may result in extra message notifications in your other Jabber apps while you have the Pings window open.
            """.trimIndent(),
            style = RiftTheme.typography.bodyPrimary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.padding(top = Spacing.mediumLarge),
        ) {
            RiftButton(
                text = "Keep disabled",
                cornerCut = ButtonCornerCut.BottomLeft,
                type = ButtonType.Secondary,
                onClick = {
                    viewModel.onPapsDialogClose()
                    viewModel.isShowingPapsChanged(false)
                },
                modifier = Modifier.weight(1f),
            )
            RiftButton(
                text = "Enable",
                onClick = { viewModel.isShowingPapsChanged(true) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PingsWindowContent(
    state: UiState,
    onChoosePapsClick: () -> Unit,
    onCheckPapsClick: () -> Unit,
    onOpenJabberClick: () -> Unit,
    onMumbleClick: (url: String) -> Unit,
) {
    Column {
        AnimatedVisibility(state.isShowingPaps == true && state.isJabberConnected) {
            AnimatedContent(state.paps, contentKey = { Triple(it?.strategic, it?.peacetime, it?.lastStrat) }) { paps ->
                if (paps != null) {
                    val pointerInteractionStateHolder = rememberPointerInteractionStateHolder()
                    Column(
                        modifier = Modifier
                            .pointerInteraction(pointerInteractionStateHolder),
                    ) {
                        val now = getNow()
                        val lastUpdated = key(now) { "Checked PAPs ${getRelativeTime(paps.timestamp, state.displayTimezone, now)}" }
                        PapsStatsRow(paps, lastUpdated)

                        AnimatedVisibility(Duration.between(paps.timestamp, now).toMinutes() > 5) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = Spacing.large, end = Spacing.large, top = Spacing.medium),
                            ) {
                                Text(
                                    text = lastUpdated,
                                    style = RiftTheme.typography.bodySecondary,
                                    modifier = Modifier.weight(1f),
                                )
                                val alpha by animateFloatAsState(if (pointerInteractionStateHolder.isHovered) 1f else 0f)
                                RiftButton(
                                    text = "Check now",
                                    isCompact = true,
                                    onClick = onCheckPapsClick,
                                    modifier = Modifier.alpha(alpha),
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedContent(state.isShowingPaps == null && state.isJabberConnected) {
            if (it) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = Spacing.large)
                        .border(1.dp, RiftTheme.colors.borderPrimaryDark)
                        .padding(Spacing.medium),
                ) {
                    Text(
                        text = "Would you like to see your PAPs here?",
                        style = RiftTheme.typography.headerPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = Spacing.medium),
                    )
                    RiftButton(
                        text = "Choose",
                        onClick = onChoosePapsClick,
                    )
                }
            }
        }

        val scrollState = rememberScrollState()
        LaunchedEffect(state.pings, scrollState.maxValue) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        ScrollbarColumn(
            scrollState = scrollState,
            contentPadding = PaddingValues(start = Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            scrollbarModifier = Modifier.padding(horizontal = Spacing.small),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = Spacing.large, top = Spacing.medium),
        ) {
            if (state.isJabberConnected) {
                state.pings.forEach { ping ->
                    when (ping) {
                        is PingUiModel.PlainText -> PlainTextPing(state.displayTimezone, ping)
                        is PingUiModel.FleetPing -> FleetPing(state.displayTimezone, ping, onMumbleClick)
                    }
                }
            }
            if (state.pings.isEmpty() || !state.isJabberConnected) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val text = if (state.isJabberConnected) {
                        "No pings received yet.\nClear skies."
                    } else {
                        "You need to be connected to Jabber to receive pings."
                    }
                    Text(
                        text = text,
                        style = RiftTheme.typography.headerPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.large),
                    )
                    if (!state.isJabberConnected) {
                        RiftButton(
                            text = "Check Jabber",
                            onClick = onOpenJabberClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PapsStatsRow(
    paps: Paps,
    lastUpdated: String,
) {
    RiftStatsRow(
        modifier = Modifier.padding(start = Spacing.medium),
    ) {
        RiftStatsRowItem(
            value = "${paps.strategic.month?.let { formatNumber(it) } ?: "?"} STR",
            text = "PAPs this month",
            color = if ((paps.strategic.month ?: 0) > 0) EveColors.successGreen else RiftTheme.colors.textPrimary,
            tooltip = buildAnnotatedString {
                withStyle(RiftTheme.typography.headlinePrimary.toSpanStyle()) {
                    appendLine("This month: ${paps.strategic.month?.let { formatNumber(it) } ?: "?"}")
                }
                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    appendLine("Last 30 days: ${paps.strategic.days30?.let { formatNumber(it) } ?: "?"}")
                }
                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    appendLine("Last 90 days: ${paps.strategic.days90?.let { formatNumber(it) } ?: "?"}")
                }
                withStyle(RiftTheme.typography.detailSecondary.toSpanStyle()) {
                    append(lastUpdated)
                }
            },
        )
        RiftStatsRowItem(
            value = "${paps.peacetime.month?.let { formatNumber(it) } ?: "?"} PCT",
            text = "PAPs this month",
            color = if ((paps.peacetime.month ?: 0) > 0) EveColors.airTurquoise else RiftTheme.colors.textPrimary,
            tooltip = buildAnnotatedString {
                withStyle(RiftTheme.typography.headlinePrimary.toSpanStyle()) {
                    appendLine("This month: ${paps.peacetime.month?.let { formatNumber(it) } ?: "?"}")
                }
                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    appendLine("Last 30 days: ${paps.peacetime.days30?.let { formatNumber(it) } ?: "?"}")
                }
                withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                    appendLine("Last 90 days: ${paps.peacetime.days90?.let { formatNumber(it) } ?: "?"}")
                }
                withStyle(RiftTheme.typography.detailSecondary.toSpanStyle()) {
                    append(lastUpdated)
                }
            },
        )
        RiftStatsRowItem(
            value = paps.lastStrat?.date ?: "N/A",
            text = "Last Strategic PAP",
            color = EveColors.airTurquoise,
            tooltip = buildAnnotatedString {
                if (paps.lastStrat != null) {
                    withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                        append("Character: ")
                    }
                    withStyle(RiftTheme.typography.bodyPrimary.toSpanStyle()) {
                        appendLine(paps.lastStrat.character)
                    }
                } else {
                    withStyle(RiftTheme.typography.bodySecondary.toSpanStyle()) {
                        appendLine("No Strategic PAPs yet")
                    }
                }
                withStyle(RiftTheme.typography.detailSecondary.toSpanStyle()) {
                    append(lastUpdated)
                }
            },
        )
    }
}

@Composable
private fun PlainTextPing(
    displayTimezone: ZoneId,
    ping: PingUiModel.PlainText,
) {
    val type = buildAnnotatedString {
        if (ping.target == null || ping.target == "all") {
            append("Announcement")
        } else {
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(ping.target.replaceFirstChar { it.uppercase() })
            }
            append(" message")
        }
        if (ping.sender != null) {
            append(" from ")
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(ping.sender)
            }
        }
    }
    val buttons = mutableListOf<RiftOpportunityCardButton>()
    buttons += RiftOpportunityCardButton(
        resource = Res.drawable.copy_16px,
        tooltip = "Copy ping",
        action = { Clipboard.copy(ping.sourceText) },
    )
    RiftOpportunityCard(
        category = RiftOpportunityCardCategory.Unclassified,
        type = RiftOpportunityCardType(type),
        solarSystemChipState = null,
        topRight = null,
        bottomContent = RiftOpportunityCardBottomContent.Timestamp(null, ping.timestamp, displayTimezone),
        buttons = buttons,
    ) {
        val descriptionStyle = if (ping.text.length <= 50) {
            RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold)
        } else {
            RiftTheme.typography.bodyPrimary
        }
        val linkStyle = SpanStyle(color = RiftTheme.colors.textLink, fontWeight = FontWeight.Bold)
        val linkifiedMessage = remember(ping.text) { annotateLinks(ping.text, linkStyle) }
        Text(
            text = linkifiedMessage,
            style = descriptionStyle,
        )
    }
}

@Composable
private fun FleetPing(
    displayTimezone: ZoneId,
    ping: PingUiModel.FleetPing,
    onMumbleClick: (url: String) -> Unit,
) {
    val type = buildAnnotatedString {
        if (ping.target == null || ping.target == "all") {
            append("Fleet")
        } else {
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(ping.target.replaceFirstChar { it.uppercase() })
            }
            append(" fleet")
        }
        if (ping.fleet != null) {
            append(" ")
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(ping.fleet)
            }
        }
        append(" under ")
        withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
            append(ping.fleetCommander.name)
        }
    }
    val buttons = mutableListOf<RiftOpportunityCardButton>()
    if (ping.doctrine?.link != null) {
        buttons += RiftOpportunityCardButton(
            resource = Res.drawable.fitting_16px,
            tooltip = "Doctrine forum thread",
            action = { ping.doctrine.link.toURIOrNull()?.openBrowser() },
        )
    }
    buttons += RiftOpportunityCardButton(
        resource = Res.drawable.copy_16px,
        tooltip = "Copy ping",
        action = { Clipboard.copy(ping.sourceText) },
    )
    if (ping.comms is Comms.Mumble) {
        buttons += RiftOpportunityCardButton(
            resource = Res.drawable.microphone,
            tooltip = "Join ${ping.comms.channel} on Mumble",
            action = { onMumbleClick(ping.comms.link) },
        )
    }
    val title = when (ping.papType) {
        PapType.Peacetime -> "Peacetime PAP"
        PapType.Strategic -> "Strategic PAP"
        is PapType.Text -> "${ping.papType.text.replaceFirstChar { it.uppercase() }} PAP"
        null -> "No PAP"
    }
    RiftOpportunityCard(
        category = ping.opportunityCategory,
        type = RiftOpportunityCardType(type),
        solarSystemChipState = ping.formupLocations,
        topRight = ping.fleetCommander,
        bottomContent = RiftOpportunityCardBottomContent.Timestamp(title, ping.timestamp, displayTimezone),
        buttons = buttons,
    ) {
        val descriptionStyle = if (ping.description.length <= 50) {
            RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold)
        } else {
            RiftTheme.typography.bodyPrimary
        }
        val linkStyle = SpanStyle(color = RiftTheme.colors.textLink, fontWeight = FontWeight.Bold)
        val linkifiedMessage = remember(ping.description) { annotateLinks(ping.description, linkStyle) }
        Text(
            text = linkifiedMessage,
            style = descriptionStyle,
            modifier = Modifier.padding(top = Spacing.mediumLarge),
        )
        if (ping.comms is Comms.Text) {
            Text(
                text = "Comms:",
                style = RiftTheme.typography.bodySecondary,
                modifier = Modifier.padding(top = Spacing.mediumLarge),
            )
            val linkifiedComms = remember(ping.comms.text) { annotateLinks(ping.comms.text, linkStyle) }
            Text(
                text = linkifiedComms,
                style = RiftTheme.typography.bodyPrimary,
            )
        }
        if (ping.doctrine != null) {
            Text(
                text = "Doctrine:",
                style = RiftTheme.typography.bodySecondary,
                modifier = Modifier.padding(top = Spacing.mediumLarge),
            )
            Text(
                text = ping.doctrine.text,
                style = RiftTheme.typography.bodyPrimary,
            )
        }
    }
}
