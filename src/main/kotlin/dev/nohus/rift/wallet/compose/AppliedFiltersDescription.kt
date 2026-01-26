package dev.nohus.rift.wallet.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.*
import dev.nohus.rift.i18n.AnnotatedStringTemplate
import dev.nohus.rift.i18n.getPluralStringSync
import dev.nohus.rift.i18n.getStringSync
import dev.nohus.rift.i18n.optionGroups
import dev.nohus.rift.utils.formatNumber
import dev.nohus.rift.wallet.TransferDirection
import dev.nohus.rift.wallet.WalletJournalItem
import dev.nohus.rift.wallet.WalletViewModel.UiState
import dev.nohus.rift.wallet.WalletViewModel.WalletTab
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppliedFiltersDescription(
    state: UiState,
    journal: List<WalletJournalItem>?,
    tab: WalletTab,
    modifier: Modifier = Modifier,
) {
    val text = buildAnnotatedString {
        val primary = RiftTheme.colors.textPrimary
        val groupBuilders = AnnotatedStringTemplate.parseGroup(stringResource(Res.string.wallet_description))
        val optionGroups = groupBuilders.optionGroups()
        optionGroups[0].apply {
            whatShouldPassBlock = AnnotatedStringTemplate.BlockParameterType.WHOLE_GROUP_TEXT
            predicate = { true }
            block = {
                if (tab == WalletTab.Transactions) {
                    append(getStringSync(Res.string.wallet_window_showing))
                } else {
                    append(getStringSync(Res.string.wallet_window_based_on))
                }
            }
        }
        val transactionsCount = journal?.size ?: 0
        optionGroups[1].apply {
            whatShouldPassBlock = AnnotatedStringTemplate.BlockParameterType.WHOLE_GROUP_TEXT
            predicate = { true }
            block = {
                withStyle(
                    style = SpanStyle(color = primary, fontWeight = FontWeight.Bold),
                ) {
                    append(getStringSync(Res.string.wallet_window_transaction_wallet_count, formatNumber(transactionsCount)))
                }
            }
        }
        optionGroups[2].apply {
            whatShouldPassBlock = AnnotatedStringTemplate.BlockParameterType.WHOLE_GROUP_TEXT
            predicate = { true }
            block = {
                when (tab) {
                    WalletTab.Transactions -> append(
                        when (state.filters.direction) {
                            TransferDirection.Income -> getPluralStringSync(Res.plurals.wallet_window_deposit, transactionsCount)
                            TransferDirection.Expense -> getPluralStringSync(Res.plurals.wallet_window_withdrawal, transactionsCount)
                            null -> getPluralStringSync(Res.plurals.wallet_window_transaction, transactionsCount)
                        },
                    )

                    else -> append(getPluralStringSync(Res.plurals.wallet_window_transaction, transactionsCount))
                }
            }
        }
        val walletsCount = journal?.map { it.wallet }?.distinct()?.size ?: 0

        optionGroups[3].apply {
            whatShouldPassBlock = AnnotatedStringTemplate.BlockParameterType.WHOLE_GROUP_TEXT
            predicate = { true }
            block = {
                withStyle(
                    style = SpanStyle(color = primary, fontWeight = FontWeight.Bold),
                ) {
                    append(getStringSync(Res.string.wallet_window_transaction_wallet_count, walletsCount))
                }
            }
        }
        optionGroups[4].apply {
            whatShouldPassBlock = AnnotatedStringTemplate.BlockParameterType.WHOLE_GROUP_TEXT
            predicate = { true }
            block = {
                append(getPluralStringSync(Res.plurals.wallet_window_wallet, walletsCount))
            }
        }
        optionGroups[5].apply {
            predicate = { tab == WalletTab.Transactions && state.filters.referenceTypes.isNotEmpty() }
            placeholders["typeNumber"] = { getStringSync(Res.string.wallet_window_type_count, state.filters.referenceTypes.size.toString()) }
            block = {
                withStyle(style = SpanStyle(color = primary, fontWeight = FontWeight.Bold)) {
                    append(it)
                }
            }
        }
        optionGroups[6].apply {
            predicate = { tab == WalletTab.Transactions && state.filters.referenceTypes.isNotEmpty() }
            block = {
                append(getPluralStringSync(Res.plurals.wallet_window_type, state.filters.referenceTypes.size))
            }
        }
        val builder = AnnotatedStringTemplate.Builder(this)
        groupBuilders.map { it.build() }.forEach { builder.addGroup(it) }
        builder.build().expand()
    }

    AnimatedContent(text) { text ->
        Text(
            text = text,
            style = RiftTheme.typography.bodySecondary,
            modifier = modifier,
        )
    }
}
