package dev.nohus.rift.alerts.list

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.alerts.Alert
import dev.nohus.rift.alerts.AlertAction
import dev.nohus.rift.alerts.AlertTrigger
import dev.nohus.rift.alerts.ChatMessageChannel
import dev.nohus.rift.alerts.GameActionType
import dev.nohus.rift.alerts.IntelChannel
import dev.nohus.rift.alerts.IntelReportLocation
import dev.nohus.rift.alerts.IntelReportType
import dev.nohus.rift.alerts.JabberMessageChannel
import dev.nohus.rift.alerts.JabberPingType
import dev.nohus.rift.alerts.JumpRange
import dev.nohus.rift.alerts.PapType
import dev.nohus.rift.alerts.PiEventType
import dev.nohus.rift.alerts.TargetedAction
import dev.nohus.rift.alerts.create.CreateAlertDialog
import dev.nohus.rift.alerts.creategroup.CreateGroupDialog
import dev.nohus.rift.alerts.list.AlertsViewModel.UiState
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ExpandChevron
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCheckbox
import dev.nohus.rift.compose.RiftDropdownWithLabel
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.contacts.ContactsRepository.Label
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.*
import dev.nohus.rift.i18n.AnnotatedStringTemplate
import dev.nohus.rift.i18n.ApplicationLocale
import dev.nohus.rift.i18n.getPluralStringSync
import dev.nohus.rift.i18n.getStringSync
import dev.nohus.rift.planetaryindustry.PlanetaryIndustryRepository.ColonyItem
import dev.nohus.rift.utils.sound.Sound
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.viewModel
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import java.nio.file.Path
import java.time.Duration
import java.util.Locale
import kotlin.io.path.nameWithoutExtension

@Composable
fun AlertsWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: AlertsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = stringResource(Res.string.alerts),
        icon = Res.drawable.window_loudspeaker_icon,
        state = windowState,
        onCloseClick = onCloseRequest,
    ) {
        AlertsWindowContent(
            state = state,
            onAlertClick = viewModel::onAlertClick,
            onGroupClick = viewModel::onGroupClick,
            onToggleAlert = viewModel::onToggleAlert,
            onGroupChange = viewModel::onGroupChange,
            onTestAlertSound = viewModel::onTestAlertSound,
            onEditAlertAction = viewModel::onEditAlertAction,
            onDeleteAlert = viewModel::onDeleteAlert,
            onCreateAlertClick = viewModel::onCreateAlertClick,
            onCreateGroupClick = viewModel::onCreateGroupClick,
            onGroupRenameClick = viewModel::onGroupRenameClick,
            onGroupDeleteClick = viewModel::onGroupDeleteClick,
            onGroupToggleAlerts = viewModel::onGroupToggleAlerts,
        )

        val isCreateAlertDialogOpen = state.isCreateAlertDialogOpen
        if (isCreateAlertDialogOpen != null) {
            CreateAlertDialog(
                inputModel = isCreateAlertDialogOpen,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseCreateAlert,
            )
        }
        val isCreateGroupDialogOpen = state.isCreateGroupDialogOpen
        if (isCreateGroupDialogOpen != null) {
            CreateGroupDialog(
                inputModel = isCreateGroupDialogOpen,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseCreateGroup,
                onConfirmClick = viewModel::onCreateGroupConfirm,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlertsWindowContent(
    state: UiState,
    onAlertClick: (id: String) -> Unit,
    onGroupClick: (name: String?) -> Unit,
    onToggleAlert: (id: String, isEnabled: Boolean) -> Unit,
    onGroupChange: (id: String, group: String?) -> Unit,
    onTestAlertSound: (id: String) -> Unit,
    onEditAlertAction: (id: String) -> Unit,
    onDeleteAlert: (id: String) -> Unit,
    onCreateAlertClick: () -> Unit,
    onCreateGroupClick: () -> Unit,
    onGroupRenameClick: (group: String) -> Unit,
    onGroupDeleteClick: (group: String) -> Unit,
    onGroupToggleAlerts: (group: String?) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        if (state.alerts.isNotEmpty()) {
            ScrollbarLazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                val nonEmptyGroups = state.alerts.mapNotNull { it.group }.toSet()
                val emptyGroups = state.groups - nonEmptyGroups
                state.alerts
                    .groupBy { it.group }
                    .let { it + emptyGroups.associateWith { emptyList() } }
                    .entries
                    .sortedWith(compareBy({ it.key == null }, { it.key }))
                    .forEach { (group, alertsInGroup) ->
                        val isExpanded = group !in state.collapsedGroups
                        stickyHeader {
                            val text = buildAnnotatedString {
                                withColor(RiftTheme.colors.textPrimary) {
                                    append(group ?: getStringSync(Res.string.default))
                                }
                                val total = alertsInGroup.size
                                val enabled = alertsInGroup.count { it.isEnabled }
                                append(" - ")
                                append(pluralStringResource(Res.plurals.alert_count, total, total))
                                if (enabled < total) {
                                    append(" - ")
                                    append(stringResource(Res.string.alert_enabled_count, enabled, enabled))
                                }
                            }
                            AlertGroupHeader(
                                name = text,
                                isEmpty = alertsInGroup.isEmpty(),
                                isExpanded = isExpanded,
                                isDefault = group == null,
                                hasEnabledAlerts = alertsInGroup.any { it.isEnabled },
                                onClick = { onGroupClick(group) },
                                onGroupToggleAlerts = { onGroupToggleAlerts(group) },
                                onGroupRenameClick = { onGroupRenameClick(group!!) },
                                onGroupDeleteClick = { onGroupDeleteClick(group!!) },
                            )
                        }
                        if (isExpanded) {
                            if (group in emptyGroups) {
                                item {
                                    EmptyGroup()
                                }
                            }
                            items(alertsInGroup, key = { it.id }) { alert ->
                                val isExpanded = alert.id == state.expandedAlert
                                AlertItem(
                                    onAlertClick = onAlertClick,
                                    alert = alert,
                                    onToggleAlert = onToggleAlert,
                                    state = state,
                                    isExpanded = isExpanded,
                                    groups = state.groups,
                                    onGroupChange = { onGroupChange(alert.id, it) },
                                    onTestAlertSound = onTestAlertSound,
                                    onEditAlertAction = onEditAlertAction,
                                    onDeleteAlert = onDeleteAlert,
                                )
                            }
                        }
                    }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(Res.string.no_alerts_defined),
                    style = RiftTheme.typography.headerPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.large),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            modifier = Modifier.align(Alignment.End),
        ) {
            if (state.alerts.isNotEmpty()) {
                RiftButton(
                    text = stringResource(Res.string.create_group),
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = onCreateGroupClick,
                )
            }
            RiftButton(
                text = stringResource(Res.string.create_alert),
                onClick = onCreateAlertClick,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.AlertGroupHeader(
    name: AnnotatedString,
    isEmpty: Boolean,
    isExpanded: Boolean,
    isDefault: Boolean,
    hasEnabledAlerts: Boolean,
    onClick: () -> Unit,
    onGroupToggleAlerts: () -> Unit,
    onGroupRenameClick: () -> Unit,
    onGroupDeleteClick: () -> Unit,
) {
    val pointerState = remember { PointerInteractionStateHolder() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .pointerInteraction(pointerState)
            .background(RiftTheme.colors.backgroundPrimary)
            .fillMaxWidth()
            .animateItem()
            .animateContentSize()
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .onClick { onClick() },
    ) {
        ExpandChevron(isExpanded = isExpanded)
        Text(
            text = name,
            style = RiftTheme.typography.headerSecondary,
            modifier = Modifier.padding(vertical = Spacing.small),
        )
        Spacer(Modifier.weight(1f))

        val buttonsAlpha by animateFloatAsState(if (pointerState.isHovered) 1f else 0f)
        if (!isEmpty) {
            RiftTooltipArea(
                text = if (hasEnabledAlerts) stringResource(Res.string.disable_all_alerts) else stringResource(Res.string.enable_all_alerts),
            ) {
                RiftImageButton(
                    resource = if (hasEnabledAlerts) Res.drawable.toggle_on_18 else Res.drawable.toggle_off_18,
                    size = 18.dp,
                    onClick = onGroupToggleAlerts,
                    modifier = Modifier.alpha(buttonsAlpha),
                )
            }
        }
        if (!isDefault) {
            RiftTooltipArea(
                text = stringResource(Res.string.rename_group),
            ) {
                RiftImageButton(
                    resource = Res.drawable.editplanicon,
                    size = 20.dp,
                    onClick = onGroupRenameClick,
                    modifier = Modifier.alpha(buttonsAlpha),
                )
            }
            RiftTooltipArea(
                text = if (isEmpty) stringResource(Res.string.delete_group) else stringResource(Res.string.delete_group_and_move_alerts),
            ) {
                RiftImageButton(
                    resource = Res.drawable.delete,
                    size = 20.dp,
                    onClick = onGroupDeleteClick,
                    modifier = Modifier.alpha(buttonsAlpha),
                )
            }
        }
        Spacer(Modifier.width(Spacing.small))
    }
}

@Composable
private fun LazyItemScope.EmptyGroup() {
    Row(
        modifier = Modifier
            .padding(vertical = Spacing.medium)
            .fillMaxWidth()
            .animateItem()
            .animateContentSize(),
    ) {
        Text(
            text = stringResource(Res.string.no_alerts_in_this_group),
            style = RiftTheme.typography.bodySecondary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.AlertItem(
    onAlertClick: (id: String) -> Unit,
    alert: Alert,
    onToggleAlert: (id: String, isEnabled: Boolean) -> Unit,
    state: UiState,
    isExpanded: Boolean,
    groups: Set<String>,
    onGroupChange: (group: String?) -> Unit,
    onTestAlertSound: (id: String) -> Unit,
    onEditAlertAction: (id: String) -> Unit,
    onDeleteAlert: (id: String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier
            .hoverBackground()
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .padding(vertical = Spacing.medium)
            .fillMaxWidth()
            .animateItem()
            .animateContentSize()
            .onClick { onAlertClick(alert.id) },
    ) {
        val alpha = if (alert.isEnabled) 1f else 0.5f
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.small),
        ) {
            RiftCheckbox(
                isChecked = alert.isEnabled,
                onCheckedChange = { onToggleAlert(alert.id, it) },
            )
            val text = getAlertText(alert, state.characters, state.sounds, state.labels)

            Text(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .alpha(alpha)
                    .padding(horizontal = Spacing.medium),
            )
        }
        if (isExpanded) {
            listOfNotNull(
                getSpecificCharactersDetailText(alert),
                getSpecificShipClassesDetailText(alert),
                getSpecificFleetCommandersDetailText(alert),
                getDecloakIgnoredKeywordsDetailText(alert),
                getSpecificColoniesDetailText(alert, state.colonies),
                getLabeledContactsDetailText(alert, state.labels),
            ).forEach {
                Row(
                    modifier = Modifier
                        .padding(horizontal = Spacing.small)
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = it,
                        modifier = Modifier
                            .weight(1f)
                            .alpha(alpha),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .padding(horizontal = Spacing.medium)
                    .fillMaxWidth(),
            ) {
                RiftDropdownWithLabel(
                    label = stringResource(Res.string.group),
                    items = (groups.sorted() + listOf(null)).toList(),
                    selectedItem = alert.group,
                    onItemSelected = onGroupChange,
                    getItemName = { it ?: "Default" },
                    maxItems = 3,
                    modifier = Modifier
                        .widthIn(max = 170.dp)
                        .padding(end = Spacing.medium),
                )
                if (alert.actions.any { it is AlertAction.Sound || it is AlertAction.CustomSound }) {
                    RiftButton(
                        text = stringResource(Res.string.test_sound),
                        type = ButtonType.Secondary,
                        cornerCut = ButtonCornerCut.None,
                        onClick = { onTestAlertSound(alert.id) },
                        modifier = Modifier.padding(end = Spacing.medium),
                    )
                }
                RiftButton(
                    text = stringResource(Res.string.edit_action),
                    type = ButtonType.Secondary,
                    cornerCut = ButtonCornerCut.None,
                    onClick = { onEditAlertAction(alert.id) },
                    modifier = Modifier.padding(end = Spacing.medium),
                )
                RiftButton(
                    text = stringResource(Res.string.delete),
                    type = ButtonType.Negative,
                    onClick = { onDeleteAlert(alert.id) },
                )
            }
        }
    }
}

@Composable
private fun getSpecificCharactersDetailText(alert: Alert): AnnotatedString? {
    return if (alert.trigger is AlertTrigger.IntelReported) {
        val specificCharacters = alert.trigger.reportTypes
            .firstOrNull { it is IntelReportType.SpecificCharacters }
        if (specificCharacters != null) {
            val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
            val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
            val characters = (specificCharacters as IntelReportType.SpecificCharacters).characters
            buildAnnotatedString {
                withStyle(secondary) {
                    append("Monitored characters: ")
                    characters.forEach { character ->
                        withStyle(primary) {
                            append(character)
                        }
                        if (character != characters.last()) {
                            append(", ")
                        }
                    }
                }
            }
        } else {
            null
        }
    } else {
        null
    }
}

@Composable
private fun getSpecificFleetCommandersDetailText(alert: Alert): AnnotatedString? {
    return if (alert.trigger is AlertTrigger.JabberPing && alert.trigger.pingType is JabberPingType.Fleet) {
        val fleetCommanders = alert.trigger.pingType.fleetCommanders
        if (fleetCommanders.isNotEmpty()) {
            val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
            val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
            buildAnnotatedString {
                withStyle(secondary) {
                    append("Fleet commanders: ")
                    fleetCommanders.forEach { character ->
                        withStyle(primary) {
                            append(character)
                        }
                        if (character != fleetCommanders.last()) {
                            append(", ")
                        }
                    }
                }
            }
        } else {
            null
        }
    } else {
        null
    }
}

@Composable
private fun getDecloakIgnoredKeywordsDetailText(alert: Alert): AnnotatedString? {
    val decloakedTrigger = (alert.trigger as? AlertTrigger.GameAction)?.actionTypes
        ?.filterIsInstance<GameActionType.Decloaked>()?.firstOrNull() ?: return null
    return if (decloakedTrigger.ignoredKeywords.isNotEmpty()) {
        val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
        val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
        buildAnnotatedString {
            withStyle(secondary) {
                append("Ignore decloaking objects containing: ")
                decloakedTrigger.ignoredKeywords.forEach { keyword ->
                    withStyle(primary) {
                        append(keyword)
                    }
                    if (keyword != decloakedTrigger.ignoredKeywords.last()) {
                        append(", ")
                    }
                }
            }
        }
    } else {
        null
    }
}

@Composable
private fun getSpecificColoniesDetailText(alert: Alert, colonies: List<ColonyItem>): AnnotatedString? {
    val colonyIds = (alert.trigger as? AlertTrigger.PlanetaryIndustry)?.coloniesFilter ?: return null
    val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
    val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
    return buildAnnotatedString {
        withStyle(secondary) {
            append("Target colonies:")
            colonyIds
                .mapNotNull { id ->
                    val colony = colonies.find { it.colony.id == id } ?: return@mapNotNull null
                    colony.colony.characterId to colony
                }.groupBy {
                    it.first
                }.forEach { (_, entries) ->
                    val items = entries.map { it.second }
                    val characterName = items.first().characterName ?: "Loading…"
                    append("\n")
                    withStyle(primary) {
                        append(characterName)
                    }
                    append(": ")
                    items.forEachIndexed { index, colony ->
                        if (index != 0) append(", ")
                        withStyle(primary) {
                            append(colony.colony.planet.name)
                        }
                    }
                }
        }
    }
}

@Composable
private fun getSpecificShipClassesDetailText(alert: Alert): AnnotatedString? {
    return if (alert.trigger is AlertTrigger.IntelReported) {
        val specificClasses = alert.trigger.reportTypes
            .firstOrNull { it is IntelReportType.SpecificShipClasses }
        if (specificClasses != null) {
            val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
            val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
            val characters = (specificClasses as IntelReportType.SpecificShipClasses).classes
            buildAnnotatedString {
                withStyle(secondary) {
                    append("Monitored ship classes: ")
                    characters.forEach { character ->
                        withStyle(primary) {
                            append(character)
                        }
                        if (character != characters.last()) {
                            append(", ")
                        }
                    }
                }
            }
        } else {
            null
        }
    } else {
        null
    }
}

@Composable
private fun getLabeledContactsDetailText(alert: Alert, labels: List<Label>): AnnotatedString? {
    return if (alert.trigger is AlertTrigger.IntelReported) {
        val labeledContacts = alert.trigger.reportTypes
            .firstOrNull { it is IntelReportType.LabeledContacts }
        if (labeledContacts != null) {
            val labels = (labeledContacts as IntelReportType.LabeledContacts).labels.mapNotNull { label ->
                labels.firstOrNull { it.owner.id == label.ownerId && it.id == label.id }
            }
            val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
            val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
            buildAnnotatedString {
                withStyle(secondary) {
                    append("Contact labels: ")
                    labels.forEachIndexed { index, label ->
                        withStyle(primary) {
                            append(label.name)
                        }
                        append(" from ")
                        withStyle(primary) {
                            append(label.owner.name)
                        }
                        if (index != labels.lastIndex) {
                            appendLine()
                        }
                    }
                }
            }
        } else {
            null
        }
    } else {
        null
    }
}

@Composable
private fun getAlertText(
    alert: Alert,
    characters: List<LocalCharacter>,
    sounds: List<Sound>,
    labels: List<Label>,
): AnnotatedString {
    val secondary = SpanStyle(color = RiftTheme.colors.textSecondary)
    val primary = SpanStyle(color = RiftTheme.colors.textPrimary)
    return buildAnnotatedString {
        val groupBuilders: MutableList<AnnotatedStringTemplate.GroupBuilder> = arrayListOf()
        withStyle(secondary) {
            when (val trigger = alert.trigger) {
                is AlertTrigger.IntelReported -> {
                    groupBuilders.addAll(intelReportedTemplateGroupBuilder(labels, characters, trigger, primary, this))
                }

                is AlertTrigger.GameAction -> {
                    groupBuilders.addAll(gameActionTemplateGroupBuilder(primary, trigger, this))
                }

                is AlertTrigger.PlanetaryIndustry -> {
                    groupBuilders.addAll(planetaryIndustryTemplateGroupBuilder(trigger, primary, this))
                }

                is AlertTrigger.ChatMessage -> {
                    groupBuilders.addAll(chatMessageTemplateGroupBuilder(trigger, primary, this))
                }

                is AlertTrigger.JabberPing -> {
                    @Suppress("DEPRECATION")
                    when (trigger.pingType) {
                        JabberPingType.Message -> {}
                        is JabberPingType.Message2 -> {
                            groupBuilders.addAll(jabberPingMessageTemplateGroupBuilder(trigger, this, primary))
                        }

                        is JabberPingType.Fleet -> {
                            groupBuilders.addAll(jabberPingTemplateGroupBuilder(trigger, primary, this))
                        }
                    }
                }

                is AlertTrigger.JabberMessage -> {
                    groupBuilders.addAll(jabberMessageTemplateGroupBuilder(trigger, this, primary))
                }

                is AlertTrigger.NoChannelActivity -> {
                    groupBuilders.addAll(noChannelActivityTemplateGroupBuilder(trigger, primary, this))
                }
            }
            if (groupBuilders.size > 2) {
                handleActionTemplateGroup(alert, sounds, primary, this, groupBuilders)
                handleCooldownGroup(alert, primary, this, groupBuilders)
            }

            val builder =
                AnnotatedStringTemplate.Builder(this)
            groupBuilders.map { it.build() }.forEach { builder.addGroup(it) }
            builder.build().expand()
        }
    }
}

@Composable
private fun intelReportedTemplateGroupBuilder(
    labels: List<Label>,
    characters: List<LocalCharacter>,
    trigger: AlertTrigger.IntelReported,
    primary: SpanStyle,
    annotatedStringBuilder: AnnotatedString.Builder,
): List<AnnotatedStringTemplate.GroupBuilder> {
    fun getTypeText(trigger: AlertTrigger.IntelReported): String {
        return trigger.reportTypes.joinToString { type ->
            when (type) {
                IntelReportType.AnyCharacter -> getStringSync(Res.string.characters_lowercase)
                is IntelReportType.SpecificCharacters -> {
                    if (type.characters.size == 1) {
                        type.characters.single()
                    } else {
                        getStringSync(Res.string.trailing_characters, type.characters.size)
                    }
                }

                IntelReportType.AnyShip -> getStringSync(Res.string.ships_lowercase)
                is IntelReportType.SpecificShipClasses -> {
                    if (type.classes.size == 1) {
                        getStringSync(Res.string.sepecific_ship_classes_one, type.classes.single())
                    } else {
                        getStringSync(Res.string.sepecific_ship_classes_other, type.classes.size)
                    }
                }

                is IntelReportType.LabeledContacts -> {
                    if (type.labels.size == 1) {
                        val label = type.labels.single()
                        val name =
                            labels.firstOrNull { it.owner.id == label.ownerId && it.id == label.id }?.name
                                ?: getStringSync(Res.string.unknown)
                        getStringSync(Res.string.labeled_character, name)
                    } else {
                        getStringSync(Res.string.mutil_labeled_character, type.labels.size)
                    }
                }

                IntelReportType.Bubbles -> getStringSync(Res.string.bubbles_lowercase)
                IntelReportType.GateCamp -> getStringSync(Res.string.gate_camp_lowercase)
                IntelReportType.Wormhole -> getStringSync(Res.string.wormholes_lowercase)
                IntelReportType.Ess -> getStringSync(Res.string.ESS)
                IntelReportType.Skyhook -> getStringSync(Res.string.skyhook_lowercase)
            }
        }
    }

    fun getLocationText(location: IntelReportLocation): String {
        return when (location) {
            is IntelReportLocation.System -> getIntelSystemText(location)
            is IntelReportLocation.AnyOwnedCharacter if location.onlyUndocked -> getIntelAnyUndockCharacterText(location)
            is IntelReportLocation.AnyOwnedCharacter -> getIntelAnyOnlineCharacterText(location)
            is IntelReportLocation.OwnedCharacter if location.onlyUndocked -> getIntelSpecificUndockCharacterText(location, characters)
            is IntelReportLocation.OwnedCharacter -> getIntelSpecificOnlineCharacterText(location, characters)
        }
    }
    return AnnotatedStringTemplate.parseGroup(stringResource(Res.string.alert_when_intel_reported))
        .also {
            val optionGroups = it.optionGroups()
            optionGroups[0].apply {
                predicate = { trigger.reportTypes.size != 1 }
                whatShouldPassBlock = AnnotatedStringTemplate.BlockParameterType.WHOLE_GROUP_TEXT
            }
            optionGroups[1].apply {
                applyStyleToGroup(this, annotatedStringBuilder, primary, getTypeText(trigger))
            }
            optionGroups[2].apply {
                applyStyleToGroup(this, annotatedStringBuilder, primary, getLocationText(trigger.reportLocation))
            }
        }
}

private fun getIntelSystemText(location: IntelReportLocation.System): String {
    return if (ApplicationLocale.current == Locale.CHINESE) {
        "在${location.systemName}${getRangePrefixText(location.jumpsRange)} "
    } else {
        "${getRangePrefixText(location.jumpsRange)} ${location.systemName}"
    }
}

private fun getIntelAnyUndockCharacterText(location: IntelReportLocation.AnyOwnedCharacter): String {
    val jumpsRange = location.jumpsRange
    return if (ApplicationLocale.current == Locale.CHINESE) {
        if (jumpsRange.min == 0 && jumpsRange.max == 0) {
            "出现任何出站角色当前位置"
        } else {
            "距离任何出站角色${getRangePrefixText(jumpsRange)}"
        }
    } else {
        "${getRangePrefixText(jumpsRange)} any undocked character's location"
    }
}

private fun getIntelSpecificUndockCharacterText(
    location: IntelReportLocation.OwnedCharacter,
    characters: List<LocalCharacter>,
): String {
    val jumpsRange = location.jumpsRange
    val character = characters.firstOrNull { it.characterId == location.characterId }?.info?.name
        ?: location.characterId.toString()
    return if (ApplicationLocale.current == Locale.CHINESE) {
        if (jumpsRange.min == 0 && jumpsRange.max == 0) {
            "出现在出站的${character}当前位置"
        } else {
            "距离出站的${character}${getRangePrefixText(jumpsRange)}"
        }
    } else {
        "${getRangePrefixText(location.jumpsRange)} $character's undocked location"
    }
}

private fun getIntelSpecificOnlineCharacterText(
    location: IntelReportLocation.OwnedCharacter,
    characters: List<LocalCharacter>,
): String {
    val jumpsRange = location.jumpsRange
    val character = characters.firstOrNull { it.characterId == location.characterId }?.info?.name
        ?: location.characterId.toString()
    return if (ApplicationLocale.current == Locale.CHINESE) {
        if (jumpsRange.min == 0 && jumpsRange.max == 0) {
            "出现在${character}当前位置"
        } else {
            "距离${character}${getRangePrefixText(jumpsRange)}"
        }
    } else {
        "${getRangePrefixText(location.jumpsRange)} $character's location"
    }
}

private fun getIntelAnyOnlineCharacterText(location: IntelReportLocation.AnyOwnedCharacter): String {
    val jumpsRange = location.jumpsRange
    return if (ApplicationLocale.current == Locale.CHINESE) {
        if (jumpsRange.min == 0 && jumpsRange.max == 0) {
            "出现任何在线角色当前位置"
        } else {
            "距离任何在线角色${getRangePrefixText(jumpsRange)}"
        }
    } else {
        "${getRangePrefixText(jumpsRange)} any online character's location"
    }
}

private fun getRangePrefixText(range: JumpRange): String {
    val (min, max) = range.min to range.max
    return if (ApplicationLocale.current == Locale.CHINESE) {
        when {
            min == 0 && max == 0 -> ""
            min == 0 -> "${max}跳内"
            min == max -> "${min}跳位"
            else -> "${min}-${max}跳之间"
        }
    } else {
        val plural = if (max > 1) "s" else ""
        if (min == 0 && max == 0) {
            "in"
        } else if (min == 0) {
            "up to $max jump$plural from"
        } else if (min == max) {
            "exactly $max jump$plural from"
        } else {
            "between $min–$max jump$plural from"
        }
    }
}

@Composable
private fun gameActionTemplateGroupBuilder(
    primary: SpanStyle,
    trigger: AlertTrigger.GameAction,
    annotatedStringBuilder: AnnotatedString.Builder,
): List<AnnotatedStringTemplate.GroupBuilder> {
    val builderList: MutableList<AnnotatedStringTemplate.GroupBuilder> = arrayListOf()
    val typeSeparatorGroup = AnnotatedStringTemplate.parseGroup(stringResource(Res.string.type_separator))

    @Composable
    fun parseTargetedActionTemplate(
        type: TargetedAction,
        stringResource: StringResource,
    ): List<AnnotatedStringTemplate.GroupBuilder> {
        return AnnotatedStringTemplate.parseGroup(stringResource(stringResource)).also {
            val optionGroups = it.optionGroups()
            optionGroups[0].apply { applyStyleToGroup(this, annotatedStringBuilder, primary) }
            optionGroups[1].apply {
                predicate = { type.nameContaining != null }
                block = {
                    annotatedStringBuilder.withStyle(primary) {
                        append(type.nameContaining)
                    }
                }
            }
        }
    }

    @Composable
    fun parseOneGroupTemplate(templateResource: StringResource): List<AnnotatedStringTemplate.GroupBuilder> =
        AnnotatedStringTemplate.parseGroup(stringResource(templateResource))
            .also {
                val optionGroups = it.optionGroups()
                optionGroups[0].apply { applyStyleToGroup(this, annotatedStringBuilder, primary) }
            }

    builderList.add(AnnotatedStringTemplate.GroupBuilder(false, stringResource(Res.string.`when`)).apply { predicate = { true } })
    trigger.actionTypes.forEachIndexed { index, type ->
        if (index != 0) {
            builderList.addAll(typeSeparatorGroup)
        }
        when (type) {
            is GameActionType.InCombat -> {
                parseTargetedActionTemplate(type, Res.string.alert_in_combat).appendToBuilderList(builderList)
            }

            is GameActionType.UnderAttack -> {
                parseTargetedActionTemplate(type, Res.string.alert_under_attack).appendToBuilderList(builderList)
            }

            is GameActionType.Attacking -> {
                parseTargetedActionTemplate(type, Res.string.alert_you_are_attacking).appendToBuilderList(builderList)
            }

            GameActionType.BeingWarpScrambled -> {
                parseOneGroupTemplate(Res.string.alert_you_are_being_warp_scrambled).appendToBuilderList(builderList)
            }

            is GameActionType.Decloaked -> {
                AnnotatedStringTemplate.parseGroup(stringResource(Res.string.alert_you_are_decloaked))
                    .also {
                        val optionGroups = it.optionGroups()
                        optionGroups[0].apply { applyStyleToGroup(this, annotatedStringBuilder, primary) }
                        optionGroups[1].apply {
                            predicate = { type.ignoredKeywords.isNotEmpty() }
                            whatShouldPassBlock = AnnotatedStringTemplate.BlockParameterType.WHOLE_GROUP_TEXT
                        }
                    }.appendToBuilderList(builderList)
            }

            is GameActionType.CombatStopped -> {
                AnnotatedStringTemplate.parseGroup(stringResource(Res.string.alert_you_are_no_longer_in_combat))
                    .also {
                        val optionGroups = it.optionGroups()
                        optionGroups[0].apply { applyStyleToGroup(this, annotatedStringBuilder, primary) }
                        optionGroups[1].apply {
                            predicate = { type.nameContaining != null }
                            placeholders["name"] = { type.nameContaining }
                            block = {
                                annotatedStringBuilder.withStyle(primary) {
                                    append(it)
                                }
                            }
                        }
                        optionGroups[2].apply {
                            val minutes = type.durationSeconds / 60
                            val text = if (minutes >= 1) {
                                pluralStringResource(Res.plurals.trailing_minutes, minutes, minutes)
                            } else {
                                pluralStringResource(Res.plurals.trailing_seconds, type.durationSeconds, type.durationSeconds)
                            }
                            applyStyleToGroup(this, annotatedStringBuilder, primary, text)
                        }
                    }.appendToBuilderList(builderList)
            }

            GameActionType.RanOutOfCharges -> {
                parseOneGroupTemplate(Res.string.alert_a_module_out_of_charges).appendToBuilderList(builderList)
            }

            is GameActionType.Custom -> {
                AnnotatedStringTemplate.parseGroup(stringResource(Res.string.alert_custom))
                    .also {
                        val optionGroups = it.optionGroups()
                        optionGroups[0].apply {
                            predicate = { type.isRegex }
                            whatShouldPassBlock = AnnotatedStringTemplate.BlockParameterType.WHOLE_GROUP_TEXT
                        }
                        optionGroups[1].apply { applyStyleToGroup(this, annotatedStringBuilder, primary, type.messageContaining) }
                    }.appendToBuilderList(builderList)
            }
        }
    }

    builderList.addAll(AnnotatedStringTemplate.parseGroup(stringResource(Res.string.alert_suffix)))
    return builderList
}

@Composable
private fun planetaryIndustryTemplateGroupBuilder(
    trigger: AlertTrigger.PlanetaryIndustry,
    primary: SpanStyle,
    builder: AnnotatedString.Builder,
): List<AnnotatedStringTemplate.GroupBuilder> {
    val groupBuilders = AnnotatedStringTemplate.parseGroup(stringResource(Res.string.alert_when_planetary_industry))
    val optionGroups = groupBuilders.optionGroups()
    optionGroups[0].apply {
        val coloniesFilter = when {
            trigger.coloniesFilter == null -> stringResource(Res.string.any_colony)
            else -> pluralStringResource(
                Res.plurals.trailing_colonies,
                trigger.coloniesFilter.size,
                trigger.coloniesFilter.size
            )
        }
        applyStyleToGroup(this, builder, primary, coloniesFilter)
    }
    optionGroups[1].apply {
        predicate = { true }
        block = {
            trigger.eventTypes.forEachIndexed { index, type ->
                if (index != 0) builder.append(getStringSync(Res.string.type_separator))
                val text = when (type) {
                    PiEventType.ExtractorInactive -> getStringSync(Res.string.alert_pi_extractor_inactive)
                    PiEventType.Idle -> getStringSync(Res.string.alert_pi_production_stopped)
                    PiEventType.NotSetup -> getStringSync(Res.string.alert_pi_setup_uncompleted)
                    PiEventType.StorageFull -> getStringSync(Res.string.alert_pi_storage_full)
                }
                builder.withStyle(primary) {
                    append(text)
                }
            }
        }
    }
    optionGroups[2].apply {
        predicate = { trigger.alertBeforeSeconds > 0 }
        block = {
            val duration = Duration.ofSeconds(trigger.alertBeforeSeconds.toLong())
            val text = when {
                duration.toHours() >= 1 -> getPluralStringSync(Res.plurals.trailing_hours, duration.toHours().toInt(), duration.toHours())
                else -> getPluralStringSync(Res.plurals.trailing_minutes, duration.toMinutes().toInt(), duration.toMinutes())
            }
            builder.withStyle(primary) {
                append(text)
            }
        }
    }

    return groupBuilders
}

@Composable
private fun chatMessageTemplateGroupBuilder(
    trigger: AlertTrigger.ChatMessage,
    primary: SpanStyle,
    builder: AnnotatedString.Builder,
): List<AnnotatedStringTemplate.GroupBuilder> {
    val groupBuilders = AnnotatedStringTemplate.parseGroup(stringResource(Res.string.alert_when_chat_message))
    val optionGroups = groupBuilders.optionGroups()
    optionGroups[0].apply {
        predicate = { trigger.messageContaining != null }
        block = {
            if (trigger.isRegex) {
                builder.append("regex ")
            }
            builder.withStyle(primary) {
                append(trigger.messageContaining)
            }
        }
    }
    optionGroups[1].apply {
        predicate = { trigger.sender != null }
        block = {
            builder.withStyle(primary) {
                append(trigger.sender)
            }
        }
    }
    optionGroups[2].apply { predicate = { trigger.isExcludingSelf } }
    optionGroups[3].apply {
        val channel = when (val channel = trigger.channel) {
            ChatMessageChannel.Any -> stringResource(Res.string.chat_message_channel_any_lowercase)
            is ChatMessageChannel.Channel -> channel.name
        }
        applyStyleToGroup(this, builder, primary, channel)
    }

    return groupBuilders
}

@Composable
private fun jabberPingMessageTemplateGroupBuilder(
    trigger: AlertTrigger.JabberPing,
    builder: AnnotatedString.Builder,
    primary: SpanStyle,
): List<AnnotatedStringTemplate.GroupBuilder> {
    val text = stringResource(Res.string.alert_when_jabber_ping_message)
    val groupBuilders = AnnotatedStringTemplate.parseGroup(text)
    val optionGroups = groupBuilders.optionGroups()
    val pingType = trigger.pingType as JabberPingType.Message2
    optionGroups[0].apply {
        predicate = { pingType.target != null }
        placeholders["target"] = { pingType.target }
        block = {
            builder.withStyle(primary) {
                append(it)
            }
        }
    }

    return groupBuilders
}

@Composable
private fun jabberPingTemplateGroupBuilder(
    trigger: AlertTrigger.JabberPing,
    primary: SpanStyle,
    builder: AnnotatedString.Builder,
): List<AnnotatedStringTemplate.GroupBuilder> {
    val text = stringResource(Res.string.alert_when_jabber_ping)
    val groupBuilders = AnnotatedStringTemplate.parseGroup(text)
    val target = (trigger.pingType as JabberPingType.Fleet).target

    @Composable
    fun applyPrimaryStyle(): (String) -> Unit = {
        builder.withStyle(primary) {
            append(it)
        }
    }

    val optionGroups = groupBuilders.optionGroups()

    optionGroups[0].apply {
        placeholders["target"] = { target }
        predicate = { target != null }
        block = applyPrimaryStyle()
    }
    optionGroups[1].apply {
        placeholders["fc"] = {
            if (trigger.pingType.fleetCommanders.size == 1) {
                getStringSync(
                    Res.string.jabber_ping_fc_one,
                    trigger.pingType.fleetCommanders.single()
                )
            } else {
                getStringSync(
                    Res.string.jabber_ping_fc_one,
                    trigger.pingType.fleetCommanders.size
                )
            }
        }
        predicate = { trigger.pingType.fleetCommanders.isNotEmpty() }
        block = applyPrimaryStyle()
    }
    optionGroups[2].apply {
        placeholders["system"] = { trigger.pingType.formupSystem }
        predicate = { trigger.pingType.formupSystem != null }
        block = applyPrimaryStyle()
    }
    optionGroups[3].apply {
        placeholders["pap"] = {
            when (trigger.pingType.papType) {
                PapType.Peacetime -> getStringSync(Res.string.jabber_ping_fleet_pap_type_peacetime)
                PapType.Strategic -> getStringSync(Res.string.jabber_ping_fleet_pap_type_strategic)
                else -> trigger.pingType.papType.toString()
            }
        }
        predicate = { trigger.pingType.papType != PapType.Any }
        block = applyPrimaryStyle()
    }
    optionGroups[4].apply {
        placeholders["doctrine"] = { trigger.pingType.doctrineContaining }
        predicate = { trigger.pingType.doctrineContaining != null }
        block = applyPrimaryStyle()
    }

    return groupBuilders
}

@Composable
private fun jabberMessageTemplateGroupBuilder(
    trigger: AlertTrigger.JabberMessage,
    builder: AnnotatedString.Builder,
    primary: SpanStyle,
): List<AnnotatedStringTemplate.GroupBuilder> {
    val text = stringResource(Res.string.alert_when_jabber_message)
    val groupBuilders = AnnotatedStringTemplate.parseGroup(text)
    val optionGroups = groupBuilders.optionGroups()

    optionGroups[0].apply {
        predicate = { trigger.messageContaining != null }
        block = {
            if (trigger.isRegex) {
                builder.append("regex ")
            }
            builder.withStyle(primary) {
                append(trigger.messageContaining)
            }
        }
    }
    optionGroups[1].apply {
        predicate = { trigger.sender != null }
        block = {
            builder.withStyle(primary) {
                append(trigger.sender)
            }
        }
    }
    optionGroups[2].apply {
        val channel = when (val channel = trigger.channel) {
            JabberMessageChannel.Any -> stringResource(Res.string.jabber_message_channel_any_lowercase)
            is JabberMessageChannel.Channel -> channel.name
            JabberMessageChannel.DirectMessage -> stringResource(Res.string.jabber_message_channel_direct_message_lowercase)
        }
        applyStyleToGroup(this, builder, primary, channel)
    }

    return groupBuilders
}

@Composable
private fun noChannelActivityTemplateGroupBuilder(
    trigger: AlertTrigger.NoChannelActivity,
    primary: SpanStyle,
    builder: AnnotatedString.Builder,
): List<AnnotatedStringTemplate.GroupBuilder> {
    val groupBuilders = AnnotatedStringTemplate.parseGroup(stringResource(Res.string.alert_when_no_channel_activity))
    val optionGroups = groupBuilders.optionGroups()
    optionGroups[0].apply {
        val channel = when (val channel = trigger.channel) {
            IntelChannel.All -> stringResource(Res.string.all_intel_channels)
            IntelChannel.Any -> stringResource(Res.string.any_intel_channels)
            is IntelChannel.Channel -> channel.name
        }
        applyStyleToGroup(this, builder, primary, channel)
    }
    optionGroups[1].apply {
        val minutes = trigger.durationSeconds / 60
        val time = if (minutes == 1) {
            pluralStringResource(Res.plurals.trailing_minutes, minutes, minutes)
        } else if (minutes > 1) {
            pluralStringResource(Res.plurals.trailing_minutes, 1, 1)
        } else {
            pluralStringResource(Res.plurals.trailing_seconds, trigger.durationSeconds, trigger.durationSeconds)
        }
        applyStyleToGroup(this, builder, primary, time)
    }

    return groupBuilders
}

private fun handleActionTemplateGroup(
    alert: Alert,
    sounds: List<Sound>,
    primary: SpanStyle,
    builder: AnnotatedString.Builder,
    groupBuilders: List<AnnotatedStringTemplate.GroupBuilder>,
) {
    val actions = alert.actions.joinToString { action ->
        when (action) {
            AlertAction.RiftNotification -> getStringSync(Res.string.alert_action_rift_notification)
            AlertAction.SystemNotification -> getStringSync(Res.string.alert_action_system_notification)
            AlertAction.PushNotification -> getStringSync(Res.string.alert_action_push_notification)
            is AlertAction.Sound -> getStringSync(Res.string.alert_action_play_sound_lowercase) + " \"${sounds.firstOrNull { it.id == action.id }?.name ?: "?"}\""
            is AlertAction.CustomSound -> getStringSync(Res.string.alert_action_play_sound_lowercase) + " ${
                Path.of(
                    action.path
                ).nameWithoutExtension
            }"
            AlertAction.ShowPing -> getStringSync(Res.string.alert_action_show_ping_lowercase)
            AlertAction.ShowColonies -> getStringSync(Res.string.alert_action_show_colonies_lowercase)
        }
    }
    val optionGroups = groupBuilders.optionGroups()
    optionGroups[optionGroups.size - 2].apply {
        applyStyleToGroup(this, builder, primary, actions)
    }
}

private fun handleCooldownGroup(
    alert: Alert,
    primary: SpanStyle,
    builder: AnnotatedString.Builder,
    groupBuilders: List<AnnotatedStringTemplate.GroupBuilder>,
) {
    val optionGroups = groupBuilders.optionGroups()
    optionGroups[optionGroups.size - 1].apply {
        placeholders["time"] = {
            when (val minutes = alert.cooldownSeconds / 60) {
                0 -> getPluralStringSync(Res.plurals.trailing_seconds, alert.cooldownSeconds, alert.cooldownSeconds)
                1 -> getPluralStringSync(Res.plurals.trailing_minutes, 1, 1)
                else -> getPluralStringSync(Res.plurals.trailing_minutes, minutes, minutes)
            }
        }
        predicate = { alert.cooldownSeconds != 0 }
        block = {
            builder.withStyle(primary) {
                append(it)
            }
        }
    }
}

private fun List<AnnotatedStringTemplate.GroupBuilder>.optionGroups(): List<AnnotatedStringTemplate.GroupBuilder> {
    return filter { it.isOption }
}

private fun List<AnnotatedStringTemplate.GroupBuilder>.appendToBuilderList(builderList: MutableList<AnnotatedStringTemplate.GroupBuilder>) {
    builderList.addAll(this)
}

/**
 * Used to help configure [AnnotatedStringTemplate.GroupBuilder].
 * Applies a given [style] to the text within a [groupBuilder].
 * The [appendText] can be optionally provided to append another text while applying the style.
 * If [appendText] is not provided, the original text from the group will be used.
 *
 * @param groupBuilder the builder for the group to which the style will be applied
 * @param annotatedStringBuilder the builder for the annotated string where the styled text will be added
 * @param style the style to apply to the text
 * @param appendText optional text to append while applying the style; if null, the original group text is used
 */
private fun applyStyleToGroup(
    groupBuilder: AnnotatedStringTemplate.GroupBuilder,
    annotatedStringBuilder: AnnotatedString.Builder,
    style: SpanStyle,
    appendText: String? = null,
) {
    groupBuilder.predicate = { true }
    groupBuilder.whatShouldPassBlock = AnnotatedStringTemplate.BlockParameterType.WHOLE_GROUP_TEXT
    groupBuilder.block = {
        annotatedStringBuilder.withStyle(style) {
            append(appendText ?: it)
        }
    }
}
