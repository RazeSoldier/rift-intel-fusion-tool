package dev.nohus.rift.characters

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import dev.nohus.rift.characters.CharactersViewModel.AuthenticationStatus
import dev.nohus.rift.characters.CharactersViewModel.CharacterItem
import dev.nohus.rift.characters.CharactersViewModel.UiState
import dev.nohus.rift.clones.Clone
import dev.nohus.rift.compose.AsyncAllianceLogo
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ButtonCornerCut
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ClickableLocation
import dev.nohus.rift.compose.ClickableShip
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDialog
import dev.nohus.rift.compose.RiftIconButton
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.RiftWindow
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.buttoniconminus
import dev.nohus.rift.generated.resources.buttoniconplus
import dev.nohus.rift.generated.resources.clone
import dev.nohus.rift.generated.resources.delete
import dev.nohus.rift.generated.resources.editplanicon
import dev.nohus.rift.generated.resources.sso
import dev.nohus.rift.generated.resources.sso_dark
import dev.nohus.rift.generated.resources.status_warning_orange
import dev.nohus.rift.generated.resources.status_warning_red
import dev.nohus.rift.generated.resources.window_characters
import dev.nohus.rift.generated.resources.window_delete_character
import dev.nohus.rift.location.CharacterLocationRepository.Location
import dev.nohus.rift.location.LocationRepository.Station
import dev.nohus.rift.location.LocationRepository.Structure
import dev.nohus.rift.network.AsyncResource
import dev.nohus.rift.network.esi.CharacterIdShip
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.sso.SsoAuthority
import dev.nohus.rift.sso.SsoDialog
import dev.nohus.rift.utils.article
import dev.nohus.rift.utils.formatIsk
import dev.nohus.rift.utils.viewModel
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import org.jetbrains.compose.resources.painterResource

@Composable
fun CharactersWindow(
    windowState: RiftWindowState,
    onCloseRequest: () -> Unit,
) {
    val viewModel: CharactersViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    RiftWindow(
        title = "Characters",
        icon = Res.drawable.window_characters,
        state = windowState,
        tuneContextMenuItems = listOf(
            ContextMenuItem.CheckboxItem(
                text = "Show clones",
                isSelected = state.isShowingClones,
                onClick = { viewModel.onIsShowingCharactersClonesChange(!state.isShowingClones) },
            ),
        ),
        onCloseClick = onCloseRequest,
    ) {
        CharactersWindowContent(
            state = state,
            onSsoClick = viewModel::onSsoClick,
            onCopySettingsClick = viewModel::onCopySettingsClick,
            onChooseDisabledClick = viewModel::onChooseDisabledClick,
            onDisableCharacterClick = viewModel::onDisableCharacterClick,
            onEnableCharacterClick = viewModel::onEnableCharacterClick,
            onDeleteCharacterClick = viewModel::onDeleteCharacterClick,
        )

        if (state.isSsoDialogOpen) {
            SsoDialog(
                inputModel = SsoAuthority.Eve,
                parentWindowState = windowState,
                onDismiss = viewModel::onCloseSso,
            )
        } else if (state.deletingCharacter != null) {
            val name = state.deletingCharacter?.info?.success?.name ?: "character ID ${state.deletingCharacter?.characterId}"
            RiftDialog(
                title = "Delete $name?",
                icon = Res.drawable.window_delete_character,
                parentState = windowState,
                state = rememberWindowState(width = 380.dp, height = Dp.Unspecified),
                onCloseClick = viewModel::onDeleteCharacterCancel,
            ) {
                DeleteCharacterDialogContent(viewModel)
            }
        }
    }
}

@Composable
private fun DeleteCharacterDialogContent(viewModel: CharactersViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = "The settings files for this character will be deleted from your installation of EVE Online, and RIFT's connection with ESI for this character will be removed.",
            style = RiftTheme.typography.bodyPrimary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            RiftButton(
                text = "Cancel",
                cornerCut = ButtonCornerCut.BottomLeft,
                type = ButtonType.Secondary,
                onClick = viewModel::onDeleteCharacterCancel,
                modifier = Modifier.weight(1f),
            )
            RiftButton(
                text = "Delete",
                type = ButtonType.Negative,
                onClick = viewModel::onDeleteCharacterConfirm,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CharactersWindowContent(
    state: UiState,
    onSsoClick: () -> Unit,
    onCopySettingsClick: () -> Unit,
    onChooseDisabledClick: () -> Unit,
    onDisableCharacterClick: (characterId: Int) -> Unit,
    onEnableCharacterClick: (characterId: Int) -> Unit,
    onDeleteCharacterClick: (characterId: Int) -> Unit,
) {
    if (state.characters.isNotEmpty()) {
        Column {
            TopRow(
                state = state,
                onSsoClick = onSsoClick,
                onCopySettingsClick = onCopySettingsClick,
                onChooseDisabledClick = onChooseDisabledClick,
            )
            CharactersList(
                state = state,
                onDisableCharacterClick = onDisableCharacterClick,
                onChooseDisabledClick = onChooseDisabledClick,
                onEnableCharacterClick = onEnableCharacterClick,
                onDeleteCharacterClick = onDeleteCharacterClick,
            )
        }
    } else {
        Text(
            text = "No characters found.\n\nMake sure the game directory is selected in settings, and that you have logged in to at least one character on this computer before.",
            style = RiftTheme.typography.titlePrimary,
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.medium),
        )
    }
}

@Composable
private fun ColumnScope.CharactersList(
    state: UiState,
    onDisableCharacterClick: (characterId: Int) -> Unit,
    onChooseDisabledClick: () -> Unit,
    onEnableCharacterClick: (characterId: Int) -> Unit,
    onDeleteCharacterClick: (characterId: Int) -> Unit,
) {
    ScrollbarLazyColumn(
        modifier = Modifier.Companion.weight(1f),
    ) {
        items(state.characters.filterNot { it.isHidden }, key = { it.characterId }) { character ->
            Box(modifier = Modifier.animateItem()) {
                CharacterRow(
                    character = character,
                    isOnline = character.characterId in state.onlineCharacters,
                    location = state.locations[character.characterId],
                    isChoosingDisabledCharacters = state.isChoosingDisabledCharacters,
                    isShowingClones = state.isShowingClones,
                    onDisableCharacterClick = onDisableCharacterClick,
                )
            }
        }
        item(key = "disabled characters") {
            Box(modifier = Modifier.animateItem()) {
                RiftTooltipArea(
                    text = "Disabled characters will not be used in RIFT.",
                    modifier = Modifier.padding(vertical = Spacing.medium),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    ) {
                        if (state.characters.any { it.isHidden }) {
                            Text(
                                text = "Disabled characters",
                                style = RiftTheme.typography.titlePrimary,
                            )
                        } else {
                            Text(
                                text = "No disabled characters",
                                style = RiftTheme.typography.titlePrimary,
                            )
                        }
                        RiftImageButton(
                            resource = Res.drawable.editplanicon,
                            size = 20.dp,
                            onClick = onChooseDisabledClick,
                        )
                    }
                }
            }
        }
        items(state.characters.filter { it.isHidden }, key = { it.characterId }) { character ->
            Box(modifier = Modifier.animateItem()) {
                HiddenCharacterRow(
                    character = character,
                    isChoosingDisabledCharacters = state.isChoosingDisabledCharacters,
                    onEnableCharacterClick = onEnableCharacterClick,
                    onDeleteCharacterClick = onDeleteCharacterClick,
                )
            }
        }
    }
}

@Composable
private fun TopRow(
    state: UiState,
    onSsoClick: () -> Unit,
    onCopySettingsClick: () -> Unit,
    onChooseDisabledClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = Modifier.padding(bottom = Spacing.medium).fillMaxWidth(),
    ) {
        AnimatedContent(state.isChoosingDisabledCharacters) { isChoosingDisabledCharacters ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                if (isChoosingDisabledCharacters) {
                    Text(
                        text = "Enable or disable characters you don't want to use.",
                        style = RiftTheme.typography.bodyPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    RiftButton(
                        text = "Done",
                        type = ButtonType.Primary,
                        onClick = onChooseDisabledClick,
                    )
                } else {
                    SsoButton(onClick = onSsoClick)
                    RiftTooltipArea(
                        text = "Copy Eve settings\n(window positions, overview, etc.)\nbetween selected characters.",
                    ) {
                        RiftButton(
                            text = "Copy settings",
                            onClick = onCopySettingsClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SsoButton(
    onClick: () -> Unit,
) {
    val pointerState = remember { PointerInteractionStateHolder() }
    Box(
        modifier = Modifier
            .pointerInteraction(pointerState)
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .padding(end = Spacing.small)
            .clickable { onClick() },
    ) {
        AnimatedContent(
            targetState = pointerState.isHovered,
            transitionSpec = {
                fadeIn(animationSpec = tween(300))
                    .togetherWith(fadeOut(animationSpec = tween(300)))
            },
        ) { isHovered ->
            val image = if (isHovered) Res.drawable.sso else Res.drawable.sso_dark
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.FillHeight,
                modifier = Modifier.height(30.dp),
            )
        }
    }
}

@Composable
private fun CharacterRow(
    character: CharacterItem,
    isOnline: Boolean,
    location: Location?,
    isChoosingDisabledCharacters: Boolean,
    isShowingClones: Boolean,
    onDisableCharacterClick: (characterId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .hoverBackground()
            .padding(Spacing.verySmall),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OnlineIndicatorBar(isOnline)
            AsyncPlayerPortrait(
                characterId = character.characterId,
                size = 64,
                modifier = Modifier.size(64.dp),
            )
            when (character.info) {
                is AsyncResource.Ready -> {
                    Column(
                        modifier = Modifier.padding(start = Spacing.medium),
                    ) {
                        RiftTooltipArea(character.info.value.corporationName) {
                            AsyncCorporationLogo(
                                corporationId = character.info.value.corporationId,
                                size = 32,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        if (character.info.value.allianceId != null) {
                            RiftTooltipArea(character.info.value.allianceName) {
                                AsyncAllianceLogo(
                                    allianceId = character.info.value.allianceId,
                                    size = 32,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .padding(horizontal = Spacing.medium)
                            .weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(IntrinsicSize.Max),
                        ) {
                            Text(
                                text = character.info.value.name,
                                style = RiftTheme.typography.titleHighlighted,
                                modifier = Modifier.weight(1f),
                            )
                            OnlineIndicatorDot(
                                isOnline = isOnline,
                                modifier = Modifier.padding(horizontal = Spacing.medium),
                            )
                            AuthenticationStatusIcon(character.authenticationStatus)
                        }
                        LocationText(location)
                        if (character.walletBalance != null) {
                            Text(
                                text = formatIsk(character.walletBalance),
                                style = RiftTheme.typography.bodyPrimary,
                            )
                        }
                    }

                    AnimatedVisibility(!isChoosingDisabledCharacters) {
                        Location(location)
                    }
                }

                is AsyncResource.Error -> {
                    Text(
                        text = "Could not load",
                        style = RiftTheme.typography.bodySecondary.copy(color = RiftTheme.colors.borderError),
                        modifier = Modifier
                            .padding(horizontal = Spacing.medium)
                            .weight(1f),
                    )
                }

                AsyncResource.Loading -> {
                    Text(
                        text = "Loading…",
                        style = RiftTheme.typography.bodySecondary,
                        modifier = Modifier
                            .padding(horizontal = Spacing.medium)
                            .weight(1f),
                    )
                }
            }

            AnimatedVisibility(isChoosingDisabledCharacters) {
                RiftTooltipArea("Disable this character") {
                    RiftIconButton(
                        icon = Res.drawable.buttoniconminus,
                        onClick = { onDisableCharacterClick(character.characterId) },
                        modifier = Modifier.padding(start = Spacing.small),
                    )
                }
            }
        }

        AnimatedVisibility(!isChoosingDisabledCharacters && isShowingClones) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.verySmall),
            ) {
                for (clone in character.clones.sortedWith(
                    compareBy(
                        { !it.isActive },
                        { it.station?.solarSystemId ?: it.structure?.solarSystemId },
                        { -it.id },
                    ),
                )) {
                    if (clone.isActive && clone.implants.isEmpty()) continue
                    Clone(clone)
                }
            }
        }
    }
}

@Composable
private fun LocationText(location: Location?) {
    if (location != null) {
        Column {
            val systemsRepository: SolarSystemsRepository by koin.inject()
            val typesRepository: TypesRepository by koin.inject()
            val systemName = systemsRepository.getSystemName(location.solarSystemId)
            val shipName = if (location.ship != null) {
                typesRepository.getType(location.ship.shipTypeId)?.name
            } else {
                null
            }

            if (systemName != null) {
                val locationId = location.station?.stationId?.toLong() ?: location.structure?.structureId
                Text(
                    text = buildAnnotatedString {
                        if (shipName != null) {
                            withColor(RiftTheme.colors.textHighlighted) {
                                append(shipName)
                            }
                        }
                        if (locationId != null) {
                            if (shipName != null) {
                                append(" docked in ")
                            } else {
                                append("Docked in ")
                            }
                            if (location.station != null) {
                                append("a ")
                                withColor(RiftTheme.colors.textHighlighted) {
                                    append("Station")
                                }
                            } else if (location.structure != null) {
                                val structureTypeName = location.structure.typeId?.let { typesRepository.getTypeName(it) } ?: "Structure"
                                append("${structureTypeName.article} ")
                                withColor(RiftTheme.colors.textHighlighted) {
                                    append(structureTypeName)
                                }
                            }
                        } else {
                            if (shipName != null) {
                                append(" in space")
                            } else {
                                append("In space")
                            }
                        }
                    },
                    style = RiftTheme.typography.bodyPrimary,
                )
            }
        }
    }
}

@Composable
fun AuthenticationStatusIcon(
    status: AuthenticationStatus,
    modifier: Modifier = Modifier,
) {
    RiftTooltipArea(
        tooltip = {
            when (status) {
                AuthenticationStatus.Authenticated -> {}
                is AuthenticationStatus.PartiallyAuthenticated -> {
                    val scopes = status.missingScopes.joinToString("\n") { it.name }
                    Column(
                        modifier = Modifier.padding(Spacing.large),
                    ) {
                        Text(
                            text = "Missing ESI scopes:",
                            style = RiftTheme.typography.bodyPrimary,
                        )
                        Text(
                            text = scopes,
                            style = RiftTheme.typography.bodySecondary,
                            modifier = Modifier.padding(vertical = Spacing.small),
                        )
                        Text(
                            text = "Some features won't work.",
                            style = RiftTheme.typography.bodyPrimary,
                        )
                    }
                }
                AuthenticationStatus.Unauthenticated -> {
                    Text(
                        text = "Not authenticated with ESI.\nClick the log in button above.",
                        style = RiftTheme.typography.bodyPrimary,
                        modifier = Modifier.padding(Spacing.large),
                    )
                }
            }
        },
        modifier = modifier,
    ) {
        val icon = when (status) {
            AuthenticationStatus.Authenticated -> null
            is AuthenticationStatus.PartiallyAuthenticated -> Res.drawable.status_warning_orange
            AuthenticationStatus.Unauthenticated -> Res.drawable.status_warning_red
        }
        AnimatedContent(icon) {
            if (it != null) {
                Box(
                    modifier = Modifier
                        .clipToBounds()
                        .size(24.dp),
                ) {
                    Image(
                        painter = painterResource(it),
                        contentDescription = null,
                        modifier = Modifier.requiredSize(36.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Clone(clone: Clone) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(Res.drawable.clone),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )

        if (clone.implants.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.weight(1f),
            ) {
                for (implant in clone.implants) {
                    RiftTooltipArea(
                        text = implant.name,
                        modifier = Modifier.size(32.dp),
                    ) {
                        AsyncTypeIcon(
                            type = implant,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }
        } else {
            Text(
                text = "No implants",
                style = RiftTheme.typography.bodySecondary,
                modifier = Modifier
                    .padding(start = Spacing.medium)
                    .weight(1f),
            )
        }

        if (clone.isActive) {
            Text(
                text = "Active clone",
                style = RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold),
            )
        } else {
            CloneLocation(clone.station, clone.structure)
        }
    }
}

@Composable
private fun HiddenCharacterRow(
    character: CharacterItem,
    isChoosingDisabledCharacters: Boolean,
    onEnableCharacterClick: (characterId: Int) -> Unit,
    onDeleteCharacterClick: (characterId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .hoverBackground()
            .padding(Spacing.verySmall),
    ) {
        AsyncPlayerPortrait(
            characterId = character.characterId,
            size = 32,
            modifier = Modifier.size(32.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            when (character.info) {
                is AsyncResource.Ready -> {
                    AsyncCorporationLogo(
                        corporationId = character.info.value.corporationId,
                        size = 32,
                        modifier = Modifier.size(32.dp),
                    )
                    if (character.info.value.allianceId != null) {
                        AsyncAllianceLogo(
                            allianceId = character.info.value.allianceId,
                            size = 32,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Text(
                        text = character.info.value.name,
                        style = RiftTheme.typography.titleSecondary,
                        modifier = Modifier
                            .padding(horizontal = Spacing.medium),
                    )
                }

                is AsyncResource.Error -> {
                    Text(
                        text = "Could not load",
                        style = RiftTheme.typography.bodySecondary.copy(color = RiftTheme.colors.borderError),
                        modifier = Modifier.padding(horizontal = Spacing.medium),
                    )
                }

                AsyncResource.Loading -> {
                    Text(
                        text = "Loading…",
                        style = RiftTheme.typography.bodySecondary,
                        modifier = Modifier.padding(horizontal = Spacing.medium),
                    )
                }
            }
        }
        AnimatedVisibility(isChoosingDisabledCharacters) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                RiftTooltipArea("Delete this character") {
                    RiftIconButton(
                        icon = Res.drawable.delete,
                        type = ButtonType.Negative,
                        cornerCut = ButtonCornerCut.None,
                        onClick = { onDeleteCharacterClick(character.characterId) },
                    )
                }
                RiftTooltipArea("Enable this character") {
                    RiftIconButton(
                        icon = Res.drawable.buttoniconplus,
                        onClick = { onEnableCharacterClick(character.characterId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineIndicatorBar(isOnline: Boolean) {
    val height by animateDpAsState(
        targetValue = if (isOnline) 64.dp else 0.dp,
        animationSpec = tween(1000),
    )
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(height)
            .background(RiftTheme.colors.onlineGreen),
    ) {}
}

@Composable
fun OnlineIndicatorDot(
    isOnline: Boolean,
    modifier: Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (isOnline) RiftTheme.colors.onlineGreen else RiftTheme.colors.offlineRed,
        animationSpec = tween(1000),
    )
    val blur by animateFloatAsState(
        targetValue = if (isOnline) 4f else 1f,
        animationSpec = tween(1000),
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .graphicsLayer(renderEffect = BlurEffect(blur, blur, edgeTreatment = TileMode.Decal))
                .border(2.dp, color, CircleShape),
        ) {}
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        ) {}
    }
}

@Composable
private fun Location(location: Location?) {
    if (location == null) return
    val systemsRepository: SolarSystemsRepository by koin.inject()
    val typesRepository: TypesRepository by koin.inject()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row {
            AnimatedContent(location, contentKey = { it.solarSystemId }) {
                val systemName = systemsRepository.getSystemName(location.solarSystemId) ?: return@AnimatedContent
                LocationIcon(location, systemName, systemsRepository, typesRepository)
            }
            AnimatedContent(location.ship) {
                ShipIcon(location.ship, typesRepository)
            }
        }
        AnimatedContent(location, contentKey = { it.solarSystemId }) {
            val systemName = systemsRepository.getSystemName(location.solarSystemId) ?: return@AnimatedContent
            Text(
                text = systemName,
                style = RiftTheme.typography.bodyLink,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 64.dp),
            )
        }
    }
}

@Composable
private fun LocationIcon(
    location: Location,
    systemName: String,
    systemsRepository: SolarSystemsRepository,
    typesRepository: TypesRepository,
) {
    val typeId = location.station?.typeId
        ?: location.structure?.typeId
        ?: systemsRepository.getSystemSunTypeId(systemName)
    val type = typesRepository.getTypeOrPlaceholder(typeId)
    val locationId = location.station?.stationId?.toLong() ?: location.structure?.structureId

    ClickableLocation(location.solarSystemId, locationId) {
        RiftTooltipArea(
            tooltip = {
                Text(
                    text = buildAnnotatedString {
                        val stationName = location.station?.name?.replace(" - ", "\n")
                        val structureName = location.structure?.name?.replace(" - ", "\n")
                        if (stationName != null) {
                            withStyle(RiftTheme.typography.bodyHighlighted.toSpanStyle()) {
                                appendLine(type.name)
                            }
                            append(stationName.trim())
                        } else if (structureName != null) {
                            withStyle(RiftTheme.typography.bodyHighlighted.toSpanStyle()) {
                                appendLine(type.name)
                            }
                            append(structureName.removePrefix(systemName).trim())
                        } else {
                            append("In space")
                        }
                    },
                    style = RiftTheme.typography.bodyPrimary,
                    modifier = Modifier.padding(Spacing.large),
                )
            },
        ) {
            AsyncTypeIcon(
                typeId = typeId,
                modifier = Modifier.size(32.dp).border(1.dp, RiftTheme.colors.borderGreyLight),
            )
        }
    }
}

@Composable
private fun ShipIcon(ship: CharacterIdShip?, typesRepository: TypesRepository) {
    if (ship == null) return
    val type = typesRepository.getType(ship.shipTypeId) ?: return

    ClickableShip(type.name, type.id) {
        RiftTooltipArea(
            tooltip = {
                Text(
                    text = buildAnnotatedString {
                        withStyle(RiftTheme.typography.bodyHighlighted.toSpanStyle()) {
                            appendLine(type.name)
                        }
                        append(ship.shipName)
                    },
                    style = RiftTheme.typography.bodyPrimary,
                    modifier = Modifier.padding(Spacing.large),
                )
            },
        ) {
            AsyncTypeIcon(
                type = type,
                modifier = Modifier.size(32.dp).border(1.dp, RiftTheme.colors.borderGreyLight),
            )
        }
    }
}

@Composable
private fun CloneLocation(station: Station?, structure: Structure?) {
    val solarSystemId = station?.solarSystemId ?: structure?.solarSystemId ?: return
    val repository: SolarSystemsRepository by koin.inject()
    val systemName = repository.getSystemName(solarSystemId) ?: return
    val locationId = station?.stationId?.toLong() ?: structure?.structureId

    ClickableLocation(solarSystemId, locationId) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            val typeId = station?.typeId ?: structure?.typeId ?: return@ClickableLocation
            val tooltip = station?.name?.replace(" - ", "\n")
                ?: structure?.name?.replace(" - ", "\n")
                ?: return@ClickableLocation
            Text(
                text = systemName,
                style = RiftTheme.typography.bodyLink,
                modifier = Modifier.widthIn(max = 50.dp),
            )
            RiftTooltipArea(
                text = tooltip,
            ) {
                AsyncTypeIcon(
                    typeId = typeId,
                    modifier = Modifier.size(32.dp).border(1.dp, RiftTheme.colors.borderGreyLight),
                )
            }
        }
    }
}
