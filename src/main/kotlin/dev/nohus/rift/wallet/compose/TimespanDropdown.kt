package dev.nohus.rift.wallet.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nohus.rift.compose.MulticolorIconType
import dev.nohus.rift.compose.RiftDropdown
import dev.nohus.rift.compose.RiftMulticolorIcon
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.wallet.WalletFilters
import dev.nohus.rift.wallet.WalletViewModel
import java.time.Duration
import java.time.Instant

@Composable
fun TimespanDropdown(
    state: WalletViewModel.UiState,
    onFiltersUpdate: (WalletFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        modifier = modifier,
    ) {
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
            onItemSelected = { onFiltersUpdate(state.filters.copy(timeSpanDays = it)) },
            getItemName = {
                if (it != Int.MAX_VALUE) "Last $it days" else "All"
            },
        )

        RiftTooltipArea(
            text = """
                ESI only provides wallet transactions from the last 30 days,
                but RIFT will remember them indefinitely.
                
                This means if you use RIFT for a year,
                you will have complete wallet history for the last year.
            """.trimIndent(),
        ) {
            RiftMulticolorIcon(
                type = MulticolorIconType.Info,
            )
        }
    }
}
