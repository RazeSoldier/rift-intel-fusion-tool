@file:OptIn(ExperimentalFoundationApi::class)

package dev.nohus.rift.wallet.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.PointerInteractionStateHolder
import dev.nohus.rift.compose.RiftImageButton
import dev.nohus.rift.compose.RiftTextField
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.VerticalGrid
import dev.nohus.rift.compose.fadingRightEdge
import dev.nohus.rift.compose.pointerInteraction
import dev.nohus.rift.compose.rememberPointerInteractionStateHolder
import dev.nohus.rift.compose.theme.Cursors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.checkmark_16px
import dev.nohus.rift.generated.resources.editplanicon
import dev.nohus.rift.generated.resources.window_wallet
import dev.nohus.rift.utils.formatIsk
import dev.nohus.rift.utils.formatIskReadable
import dev.nohus.rift.utils.multiplyBrightness
import dev.nohus.rift.utils.toggle
import dev.nohus.rift.wallet.WalletDivisionsRepository
import dev.nohus.rift.wallet.WalletFilters
import dev.nohus.rift.wallet.WalletRepository.WalletBalance
import dev.nohus.rift.wallet.WalletType
import dev.nohus.rift.wallet.WalletViewModel.LoadedData
import dev.nohus.rift.wallet.WalletViewModel.UiState
import org.jetbrains.compose.resources.painterResource
import java.time.Instant

@Composable
fun WalletsContent(
    state: UiState,
    data: LoadedData,
    onFiltersUpdate: (WalletFilters) -> Unit,
) {
    Column {
        Text(
            text = "Choose wallets to filter transactions and insights",
            style = RiftTheme.typography.bodySecondary,
            modifier = Modifier.padding(bottom = Spacing.medium),
        )

        ScrollbarColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            contentPadding = PaddingValues(end = Spacing.medium),
        ) {
            fun updateFilters(new: List<WalletType>) {
                onFiltersUpdate(state.filters.copy(walletTypes = new))
            }
            val filters = state.filters.walletTypes

            val characterBalances = data.balances.filterIsInstance<WalletBalance.Character>()
                .associate { it.characterId to it.balance }

            VerticalGrid(
                minColumnWidth = 200.dp,
                horizontalSpacing = Spacing.medium,
                verticalSpacing = Spacing.medium,
            ) {
                WalletCard(
                    icon = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.2f)),
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.window_wallet),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    },
                    name = "All character wallets",
                    isSelected = WalletType.Character in filters,
                    onClick = {
                        val updated = filters
                            .toggle(WalletType.Character)
                            .filter { it !is WalletType.SpecificCharacter }
                        updateFilters(updated)
                    },
                    amount = characterBalances.values.sum(),
                    showCents = state.showCents,
                )

                data.characters
                    .associateWith { characterBalances[it.id] ?: 0.0 }
                    .entries
                    .sortedByDescending { (_, balance) -> balance }
                    .forEach { (character, balance) ->
                        val isSelected = WalletType.SpecificCharacter(character.id) in filters ||
                            WalletType.Character in filters

                        WalletCard(
                            icon = {
                                AsyncPlayerPortrait(
                                    characterId = character.id,
                                    size = 64,
                                    modifier = Modifier.size(48.dp),
                                )
                            },
                            name = character.name,
                            isSelected = isSelected,
                            onClick = {
                                val updated = filters
                                    .toggle(WalletType.SpecificCharacter(character.id))
                                    .filter { it !is WalletType.Character }
                                updateFilters(updated)
                            },
                            amount = balance,
                            showCents = state.showCents,
                        )
                    }
            }

            val corporations = state.availableWalletFilters.corporations
            corporations.forEach { corporation ->
                Divider(color = RiftTheme.colors.divider)
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    val divisionBalances = data.balances.filterIsInstance<WalletBalance.Corporation>()
                        .filter { it.corporationId == corporation.id }
                        .associate { it.divisionId to it.balance }

                    val walletDivisionsRepository: WalletDivisionsRepository = remember { koin.get() }

                    VerticalGrid(
                        minColumnWidth = 200.dp,
                        horizontalSpacing = Spacing.medium,
                        verticalSpacing = Spacing.medium,
                    ) {
                        WalletCard(
                            icon = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.2f)),
                                ) {
                                    AsyncCorporationLogo(
                                        corporationId = corporation.id,
                                        size = 64,
                                        modifier = Modifier.size(48.dp),
                                    )
                                }
                            },
                            name = corporation.name,
                            isSelected = WalletType.SpecificCorporation(corporation.id) in filters,
                            onClick = {
                                val updated = filters
                                    .toggle(WalletType.SpecificCorporation(corporation.id))
                                    .filter { it !is WalletType.Corporation }
                                    .filter { !(it is WalletType.SpecificCorporationDivision && it.corporationId == corporation.id) }
                                updateFilters(updated)
                            },
                            amount = divisionBalances.values.sum(),
                            showCents = state.showCents,
                        )

                        (1..7).forEach { divisionId ->
                            val balance = divisionBalances[divisionId] ?: 0.0
                            var nameKey by remember { mutableStateOf(Instant.now()) }
                            val name = remember(nameKey) { walletDivisionsRepository.getDivisionNameOrDefault(corporation.id, divisionId) }
                            val editableName = remember(nameKey) { walletDivisionsRepository.getDivisionName(corporation.id, divisionId) }
                            val isSelected = WalletType.SpecificCorporationDivision(corporation.id, divisionId) in filters ||
                                WalletType.SpecificCorporation(corporation.id) in filters ||
                                WalletType.Corporation in filters

                            WalletCard(
                                icon = {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.2f)),
                                    ) {
                                        Text(
                                            text = divisionId.toString(),
                                            style = RiftTheme.typography.displayPrimary.copy(fontSize = 32.sp),
                                        )
                                    }
                                },
                                name = name,
                                isSelected = isSelected,
                                editableName = editableName,
                                onNameChange = {
                                    walletDivisionsRepository.setDivisionName(corporation.id, divisionId, it)
                                    nameKey = Instant.now()
                                },
                                onClick = {
                                    val updated = filters
                                        .toggle(WalletType.SpecificCorporationDivision(corporation.id, divisionId))
                                        .filter { it !is WalletType.Corporation }
                                        .filter { !(it is WalletType.SpecificCorporation && it.corporationId == corporation.id) }
                                    updateFilters(updated)
                                },
                                amount = balance,
                                showCents = state.showCents,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletCard(
    icon: @Composable () -> Unit,
    name: String,
    isSelected: Boolean,
    editableName: String? = null,
    onNameChange: ((String) -> Unit)? = null,
    onClick: () -> Unit,
    amount: Double,
    showCents: Boolean,
) {
    val pointerInteractionStateHolder = rememberPointerInteractionStateHolder()
    WalletCardBox(pointerInteractionStateHolder, isSelected, onClick) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                var isEditing by remember { mutableStateOf(false) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    var text by remember { mutableStateOf(editableName ?: "") }
                    if (isEditing && onNameChange != null) {
                        RiftTextField(
                            text = text,
                            placeholder = "Division name",
                            onTextChanged = {
                                text = it.take(25)
                            },
                            height = 18.dp,
                            modifier = Modifier
                                .onKeyEvent {
                                    when (it.key) {
                                        Key.Enter -> {
                                            onNameChange(text)
                                            isEditing = false
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                .padding(end = Spacing.small)
                                .weight(1f),
                        )
                    } else {
                        Text(
                            text = name,
                            style = RiftTheme.typography.bodyPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            softWrap = false,
                            modifier = Modifier
                                .padding(end = Spacing.small)
                                .fadingRightEdge()
                                .padding(end = Spacing.large)
                                .weight(1f),
                        )
                    }
                    if (onNameChange != null) {
                        RiftImageButton(
                            resource = if (isEditing) Res.drawable.checkmark_16px else Res.drawable.editplanicon,
                            size = 16.dp,
                            onClick = {
                                if (isEditing) onNameChange(text)
                                isEditing = !isEditing
                            },
                        )
                    }
                }
                Divider(
                    color = Color.White.copy(alpha = 0.15f),
                )
                val formatted = if (pointerInteractionStateHolder.isHovered) {
                    formatIsk(amount, showCents)
                } else {
                    formatIskReadable(amount.toLong())
                }
                Text(
                    text = formatted,
                    style = RiftTheme.typography.bodyPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                    modifier = Modifier
                        .animateContentSize()
                        .fadingRightEdge()
                        .padding(end = Spacing.large)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun WalletCardBox(
    pointerInteractionStateHolder: PointerInteractionStateHolder,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .pointerInteraction(pointerInteractionStateHolder)
            .pointerHoverIcon(PointerIcon(Cursors.pointerInteractive))
            .onClick { onClick() }
            .width(IntrinsicSize.Max)
            .height(IntrinsicSize.Max),
    ) {
        val shape = CutCornerShape(bottomEnd = 8.dp)
        val color = Color(0xFF6E966B)
        val alpha by animateFloatAsState(
            if (isSelected) {
                0.5f
            } else if (pointerInteractionStateHolder.isHovered) {
                0.3f
            } else {
                0.1f
            },
        )
        Box(
            modifier = Modifier
                .alpha(alpha)
                .clip(shape)
                .background(color.multiplyBrightness(2f)),
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 2.dp)
                    .clip(shape)
                    .background(color),
            ) {
                Spacer(Modifier.fillMaxSize())
            }
        }
        Box(
            modifier = Modifier
                .padding(end = 2.dp)
                .padding(Spacing.small),
        ) {
            content()
        }
    }
}
