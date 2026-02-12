package dev.nohus.rift.wallet.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.wallet_window_all_transactions_filtered_out
import dev.nohus.rift.generated.resources.wallet_window_no_transactions
import dev.nohus.rift.wallet.WalletViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun EmptyState(state: WalletViewModel.UiState) {
    val isFiltering = state.filters.direction != null ||
        state.filters.walletTypes.isNotEmpty() ||
        state.filters.referenceTypes.isNotEmpty() ||
        state.filters.search != null
    val text = if (isFiltering) stringResource(Res.string.wallet_window_all_transactions_filtered_out) else stringResource(Res.string.wallet_window_no_transactions)
    Text(
        text = text,
        style = RiftTheme.typography.displaySecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.large),
    )
}
