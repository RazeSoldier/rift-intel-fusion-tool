package dev.nohus.rift.wallet.compose

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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ClickableLocation
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.VerticalGrid
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.evermarks
import dev.nohus.rift.repositories.StationsRepository
import dev.nohus.rift.utils.formatNumber
import dev.nohus.rift.utils.multiplyBrightness
import dev.nohus.rift.wallet.WalletViewModel.LoadedData
import org.jetbrains.compose.resources.painterResource

@Composable
fun LoyaltyPointsContent(
    data: LoadedData,
) {
    Column {
        if (data.loyaltyPointBalances.isNotEmpty()) {
            ScrollbarColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                contentPadding = PaddingValues(end = Spacing.medium),
            ) {
                Text(
                    text = "Loyalty points and the closest loyalty point store for each character",
                    style = RiftTheme.typography.bodySecondary,
                    modifier = Modifier.padding(bottom = Spacing.medium),
                )

                data.loyaltyPointBalances.sortedByDescending { it.balances.sumOf { it.balance } }.forEach { balances ->
                    val characterName = data.characters.firstOrNull { it.id == balances.characterId }
                        ?.name ?: balances.characterId.toString()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        AsyncPlayerPortrait(
                            characterId = balances.characterId,
                            size = 64,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            text = characterName,
                            style = RiftTheme.typography.headlinePrimary.copy(fontWeight = FontWeight.Bold),
                        )
                    }

                    if (balances.balances.isNotEmpty()) {
                        VerticalGrid(
                            minColumnWidth = 240.dp,
                            horizontalSpacing = Spacing.medium,
                            verticalSpacing = Spacing.medium,
                        ) {
                            val (evermarks, loyaltyPoints) = balances.balances.partition { it.corporationId == 1000419 }
                            evermarks.forEach {
                                LoyaltyPointsBox {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.2f)),
                                            ) {
                                                Image(
                                                    painter = painterResource(Res.drawable.evermarks),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(42.dp),
                                                )
                                            }
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                            ) {
                                                Text(
                                                    text = "EverMarks",
                                                    style = RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                                                )
                                                Text(
                                                    text = formatNumber(it.balance),
                                                    style = RiftTheme.typography.bodyPrimary,
                                                )
                                            }
                                        }
                                        Divider(color = Color.White.copy(alpha = 0.15f))
                                        Store(it.closestLoyaltyPointStore)
                                    }
                                }
                            }
                            loyaltyPoints.sortedByDescending { it.balance }.forEach {
                                LoyaltyPointsBox {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            AsyncCorporationLogo(
                                                corporationId = it.corporationId,
                                                size = 64,
                                                modifier = Modifier.size(48.dp),
                                            )
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(Spacing.small),
                                            ) {
                                                Text(
                                                    text = it.corporationName,
                                                    style = RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                                                )
                                                Text(
                                                    text = formatNumber(it.balance),
                                                    style = RiftTheme.typography.bodyPrimary,
                                                )
                                            }
                                        }
                                        Divider(color = Color.White.copy(alpha = 0.15f))
                                        Store(it.closestLoyaltyPointStore)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No loyalty points",
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
            }
        } else {
            Text(
                text = "No characters with loyalty points",
                style = RiftTheme.typography.displaySecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.large),
            )
        }
    }
}

@Composable
private fun Store(station: StationsRepository.Station?) {
    station?.let { store ->
        ClickableLocation(
            systemId = store.systemId,
            locationId = store.id.toLong(),
            locationTypeId = store.typeId,
            locationName = store.name,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncTypeIcon(
                    typeId = store.typeId,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = store.name,
                    style = RiftTheme.typography.detailSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LoyaltyPointsBox(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(IntrinsicSize.Max),
    ) {
        val shape = CutCornerShape(bottomEnd = 8.dp)
        val color = Color(0xFF6E966B)
        val alpha = 0.1f
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
