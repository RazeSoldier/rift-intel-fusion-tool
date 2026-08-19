package dev.nohus.rift.charactersettings.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nohus.rift.charactersettings.io.ReadCharacterSettingsUseCase.CharacterSettings
import dev.nohus.rift.compose.CharacterTooltip
import dev.nohus.rift.compose.ClickableCharacter
import dev.nohus.rift.compose.FlagIcon
import dev.nohus.rift.compose.LoadingSpinnerAmbient
import dev.nohus.rift.compose.RequirementIcon
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.ScrollbarLazyColumn
import dev.nohus.rift.compose.hoverBackground
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.delete
import dev.nohus.rift.generated.resources.deleteicon
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository.CharacterDetails

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun Watchlist(
    settings: CharacterSettings,
    characterName: String,
    characterId: Int?,
    isCharacterLookupComplete: Boolean,
    onCharacterNameChanged: (String) -> Unit,
    onWatchlistUpdated: (Int, Color?) -> Unit,
    onWatchlistCharacterRemoved: (Int) -> Unit,
) {
    val watchlistColors = listOf(
        Color(1.0f, 0.7f, 0.0f),
        Color(1.0f, 0.35f, 0.0f),
        Color(0.75f, 0.0f, 0.0f),
        Color(0.1f, 0.6f, 0.1f),
        Color(0.0f, 0.63f, 0.57f),
        Color(0.2f, 0.5f, 1.0f),
        Color(0.0f, 0.15f, 0.6f),
        Color(0.0f, 0.0f, 0.0f),
        Color(0.7f, 0.7f, 0.7f),
    )

    val characterDetailsRepository: CharacterDetailsRepository = remember { koin.get() }
    val characters: Map<Int, CharacterDetails?>? by produceState(null, settings.watchlistColors.keys) {
        value = characterDetailsRepository.getCharacterDetails(Originator.DataPreloading, settings.watchlistColors.keys)
    }
    var sortBy by remember { mutableStateOf(WatchlistSort.CharacterName) }
    val sortedWatchlist = characters?.let { characters ->
        settings.watchlistColors.entries.sortedWith(
            when (sortBy) {
                WatchlistSort.CharacterName -> compareBy { characters[it.key]?.name ?: it.key.toString() }
                WatchlistSort.Color -> compareBy { watchlistColors.indexOf(it.value).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
            }
        )
    }

    ScrollbarLazyColumn {
        item {
            Text(
                text = "Here are all characters saved in your fleet watchlist. You can add and remove from your watchlist, and set colors. In-game, only characters that are actively in a fleet with you show in the watchlist window.",
                style = RiftTheme.typography.bodySecondary,
                modifier = Modifier.padding(bottom = Spacing.medium),
            )
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = Spacing.medium),
            ) {
                Text("Sort by", style = RiftTheme.typography.bodySecondary)
                RiftDropdown(
                    items = WatchlistSort.entries,
                    selectedItem = sortBy,
                    onItemSelected = { sortBy = it },
                    getItemName = { it.label },
                    modifier = Modifier.padding(horizontal = Spacing.medium),
                )

                var characterName by remember(characterName) { mutableStateOf(characterName) }
                RiftTextField(
                    text = characterName,
                    placeholder = "Character name",
                    onTextChanged = {
                        characterName = it
                        onCharacterNameChanged(it)
                    },
                    modifier = Modifier.width(150.dp),
                )
                AnimatedVisibility(characterName.isNotBlank() && isCharacterLookupComplete) {
                    RequirementIcon(
                        isFulfilled = characterId != null,
                        fulfilledTooltip = "Character found",
                        notFulfilledTooltip = "Character not found",
                        modifier = Modifier.padding(start = Spacing.medium),
                    )
                }
                RiftButton(
                    text = "Add to watchlist",
                    isEnabled = characterId != null,
                    onClick = {
                        characterId?.let { onWatchlistUpdated(it, null) }
                        characterName = ""
                        onCharacterNameChanged(characterName)
                    },
                    modifier = Modifier.padding(start = Spacing.medium),
                )
            }
        }

        if (sortedWatchlist == null) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.large),
                ) {
                    LoadingSpinnerAmbient()
                }
            }
        } else {
            items(sortedWatchlist, key = { it.key }) { entry ->
                val character = characters?.get(entry.key)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.animateItem().hoverBackground(),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background((entry.value ?: Color.Transparent).copy(alpha = 0.25f))
                            .padding(horizontal = Spacing.small)
                            .padding(vertical = Spacing.verySmall),
                    ) {
                        ClickableCharacter(character?.characterId) {
                            CharacterTooltip(character) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = Spacing.verySmall),
                                ) {
                                    if (character != null) FlagIcon(standing = character.standingLevel)
                                    Text(
                                        text = character?.name ?: entry.key.toString(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = RiftTheme.typography.bodyPrimary,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.verySmall),
                    ) {
                        watchlistColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .border(1.dp, RiftTheme.colors.borderGreyLight)
                                    .background(color)
                                    .size(16.dp)
                                    .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
                                    .onClick { onWatchlistUpdated(entry.key, color) },
                            )
                        }
                        RiftTooltipArea(text = "Clear color") {
                            RiftImageButton(
                                resource = Res.drawable.deleteicon,
                                size = 20.dp,
                                onClick = { onWatchlistUpdated(entry.key, null) },
                            )
                        }
                        RiftTooltipArea(text = "Remove from watchlist") {
                            RiftImageButton(
                                resource = Res.drawable.delete,
                                size = 20.dp,
                                onClick = { onWatchlistCharacterRemoved(entry.key) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class WatchlistSort(val label: String) {
    CharacterName("Character name"),
    Color("Color"),
}
