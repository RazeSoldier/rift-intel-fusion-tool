package dev.nohus.rift.structures.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncCharacterPortrait
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.InfiniteScrollingCarousel
import dev.nohus.rift.compose.LoadingBox
import dev.nohus.rift.compose.LoadingSpinnerAmbient
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.VerticalGrid
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.structures.EquinoxStructuresRepository
import dev.nohus.rift.structures.EquinoxStructuresRepository.LoadingStage

@Composable
fun StructuresLoadingProgress(
    loading: EquinoxStructuresRepository.LoadingState,
    stage: LoadingStage,
) {
    ScrollbarColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(Spacing.large),
    ) {
        LoadingSpinnerAmbient()
        Spacer(Modifier.height(Spacing.medium))

        val title = when (stage) {
            LoadingStage.LoadingMercenaryDens -> "Loading mercenary dens…"
            LoadingStage.LoadingSkyhooks -> "Loading skyhooks…"
            LoadingStage.LoadingSovHubs -> "Loading sovereignty hubs…"
            LoadingStage.LoadingDetails -> "Loading structure details…"
        }
        Text(
            text = title,
            style = RiftTheme.typography.headlinePrimary,
        )

        Spacer(Modifier.height(Spacing.large))

        VerticalGrid(
            minColumnWidth = 300.dp,
            horizontalSpacing = Spacing.medium,
            verticalSpacing = Spacing.medium,
        ) {
            if (loading.mercenaryDensTotal != null && loading.mercenaryDensTotal > 0) {
                val mercenaryDensPercentage = (loading.mercenaryDensLoaded.toFloat() / (loading.mercenaryDensTotal ?: 1)).coerceIn(0f, 1f)
                LoadingBox(mercenaryDensPercentage) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncTypeIcon(
                            typeId = 85230,
                            modifier = Modifier.size(48.dp),
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            Text(
                                text = "Mercenary Dens",
                                style = RiftTheme.typography.bodyPrimary,
                            )

                            Divider(color = Color.White.copy(alpha = 0.15f))

                            val text = if (loading.mercenaryDensLoaded == loading.mercenaryDensTotal) {
                                "${loading.mercenaryDensLoaded} mercenary dens loaded"
                            } else {
                                "${loading.mercenaryDensLoaded}/${loading.mercenaryDensTotal} mercenary dens loaded…"
                            }
                            Text(
                                text = text,
                                style = if (loading.mercenaryDensLoaded == loading.mercenaryDensTotal) RiftTheme.typography.bodySecondary else RiftTheme.typography.bodyPrimary,
                            )
                        }
                    }
                }
            }

            if (loading.skyhooksTotal != null && loading.skyhooksTotal > 0) {
                val skyhooksPercentage = (loading.skyhooksLoaded.toFloat() / (loading.skyhooksTotal ?: 1)).coerceIn(0f, 1f)
                LoadingBox(skyhooksPercentage) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncTypeIcon(
                            typeId = 81080,
                            modifier = Modifier.size(48.dp),
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            Text(
                                text = "Skyhooks",
                                style = RiftTheme.typography.bodyPrimary,
                            )

                            Divider(color = Color.White.copy(alpha = 0.15f))

                            val text = if (loading.skyhooksLoaded == loading.skyhooksTotal) {
                                "${loading.skyhooksLoaded} skyhooks loaded"
                            } else {
                                "${loading.skyhooksLoaded}/${loading.skyhooksTotal} skyhooks loaded…"
                            }
                            Text(
                                text = text,
                                style = if (loading.skyhooksLoaded == loading.skyhooksTotal) RiftTheme.typography.bodySecondary else RiftTheme.typography.bodyPrimary,
                            )
                        }
                    }
                }
            }

            if (loading.sovHubsTotal != null && loading.sovHubsTotal > 0) {
                val sovHubsPercentage = (loading.sovHubsLoaded.toFloat() / (loading.sovHubsTotal ?: 1)).coerceIn(0f, 1f)
                LoadingBox(sovHubsPercentage) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncTypeIcon(
                            typeId = 32458,
                            modifier = Modifier.size(48.dp),
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            Text(
                                text = "Sovereignty Hubs",
                                style = RiftTheme.typography.bodyPrimary,
                            )

                            Divider(color = Color.White.copy(alpha = 0.15f))

                            val text = if (loading.sovHubsLoaded == loading.sovHubsTotal) {
                                "${loading.sovHubsLoaded} sovereignty hubs loaded"
                            } else {
                                "${loading.sovHubsLoaded}/${loading.sovHubsTotal} sovereignty hubs loaded…"
                            }
                            Text(
                                text = text,
                                style = if (loading.sovHubsLoaded == loading.sovHubsTotal) RiftTheme.typography.bodySecondary else RiftTheme.typography.bodyPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
