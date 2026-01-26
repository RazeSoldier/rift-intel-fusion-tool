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
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.wallet_window_timespan_dropdown_tooltip
import dev.nohus.rift.generated.resources.wallet_window_last_xx_hours
import dev.nohus.rift.generated.resources.wallet_window_last_xx_days
import dev.nohus.rift.i18n.getStringSync
import dev.nohus.rift.wallet.WalletFilters
import dev.nohus.rift.wallet.WalletViewModel
import org.jetbrains.compose.resources.stringResource
import java.time.Duration

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
        RiftDropdown(
            items = state.availableTimestamps,
            selectedItem = state.filters.timeSpan,
            onItemSelected = { onFiltersUpdate(state.filters.copy(timeSpan = it)) },
            getItemName = {
                if (it < Duration.ofDays(2)) {
                    getStringSync(Res.string.wallet_window_last_xx_hours, it.toHours())
                } else {
                    getStringSync(Res.string.wallet_window_last_xx_days, it.toDays())
                }
            },
        )

        RiftTooltipArea(stringResource(Res.string.wallet_window_timespan_dropdown_tooltip),
        ) {
            RiftMulticolorIcon(
                type = MulticolorIconType.Info,
            )
        }
    }
}
