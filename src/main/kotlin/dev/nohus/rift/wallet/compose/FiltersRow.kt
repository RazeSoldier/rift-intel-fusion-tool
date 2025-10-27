package dev.nohus.rift.wallet.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.ButtonType
import dev.nohus.rift.compose.ContextMenuItem
import dev.nohus.rift.compose.LoadingSpinner
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftContextMenuPopup
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.utils.toggle
import dev.nohus.rift.wallet.TransferDirection
import dev.nohus.rift.wallet.WalletFilters
import dev.nohus.rift.wallet.WalletViewModel
import dev.nohus.rift.wallet.getReferenceTypeName
import java.time.Duration
import java.time.Instant

@Composable
fun FiltersRow(
    state: WalletViewModel.UiState,
    onFiltersUpdate: (WalletFilters) -> Unit,
    onReloadClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier
            .padding(bottom = Spacing.medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            fun updateFilters(update: WalletFilters.() -> WalletFilters) {
                onFiltersUpdate(state.filters.update())
            }

            RiftDropdown(
                items = listOf(null, TransferDirection.Income, TransferDirection.Expense),
                selectedItem = state.filters.direction,
                onItemSelected = { updateFilters { copy(direction = it) } },
                getItemName = {
                    when (it) {
                        null -> "All"
                        TransferDirection.Income -> "Income"
                        TransferDirection.Expense -> "Expenses"
                    }
                },
            )

            val referenceTypesFilterItems = buildList<ContextMenuItem> {
                add(
                    ContextMenuItem.CheckboxItem(
                        text = "All types",
                        isSelected = state.filters.referenceTypes.isEmpty(),
                        onClick = { updateFilters { copy(referenceTypes = emptyList()) } },
                    ),
                )
                val (marketTransactionTypes, otherTypes) = state.availableWalletFilters.referenceTypes.partition {
                    it in listOf(
                        "market_transaction",
                        "market_escrow",
                    )
                }
                if (marketTransactionTypes.isNotEmpty()) {
                    add(ContextMenuItem.DividerItem)
                    add(ContextMenuItem.HeaderItem("Market Transactions"))
                    marketTransactionTypes.forEach { type ->
                        add(
                            ContextMenuItem.CheckboxItem(
                                text = getReferenceTypeName(type),
                                isSelected = type in state.filters.referenceTypes,
                                onClick = {
                                    val updated = state.filters.referenceTypes.toggle(type)
                                    updateFilters { copy(referenceTypes = updated) }
                                },
                            ),
                        )
                    }
                }
                if (otherTypes.isNotEmpty()) {
                    add(ContextMenuItem.DividerItem)
                    add(ContextMenuItem.HeaderItem("Other Types"))
                    otherTypes.forEach { type ->
                        add(
                            ContextMenuItem.CheckboxItem(
                                text = getReferenceTypeName(type),
                                isSelected = type in state.filters.referenceTypes,
                                onClick = {
                                    val updated = state.filters.referenceTypes.toggle(type)
                                    updateFilters { copy(referenceTypes = updated) }
                                },
                            ),
                        )
                    }
                }
            }
            Box(contentAlignment = Alignment.BottomStart) {
                var isShown by remember { mutableStateOf(false) }
                RiftButton(
                    text = "Types",
                    onClick = { isShown = true },
                )
                if (isShown) {
                    val offset = with(LocalDensity.current) {
                        32.dp.toPx().toInt()
                    }
                    RiftContextMenuPopup(
                        items = referenceTypesFilterItems,
                        offset = IntOffset(0, offset),
                        onDismissRequest = { isShown = false },
                    )
                }
            }

            val timespanDays = state.journalHistoryFrom?.let { from ->
                Duration.between(from, Instant.now()).toDays().toInt() + 1
            } ?: 0
            RiftDropdown(
                items = buildList {
                    repeat(timespanDays / 30) {
                        add((it + 1) * 30)
                    }
                    if (timespanDays % 30 != 0) {
                        add(timespanDays)
                    }
                },
                selectedItem = state.filters.timeSpanDays,
                onItemSelected = { updateFilters { copy(timeSpanDays = it) } },
                getItemName = {
                    if (it != Int.MAX_VALUE) "Last $it days" else "All"
                },
            )

            ReloadButton(isLoading = state.isLoading, onReloadClick = onReloadClick)
        }
    }
}

@Composable
private fun ReloadButton(
    isLoading: Boolean,
    onReloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
//        if (corporations.isNotEmpty()) {
//            Text("Delayed up to 10 minutes")
//        }
        AnimatedContent(
            isLoading,
            modifier = Modifier
                .height(36.dp),
        ) {
            if (it) {
                LoadingSpinner(
                    modifier = Modifier.size(36.dp),
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.height(36.dp),
                ) {
                    RiftButton(
                        text = "Reload",
                        type = ButtonType.Secondary,
                        onClick = onReloadClick,
                    )
                }
            }
        }
    }
}
