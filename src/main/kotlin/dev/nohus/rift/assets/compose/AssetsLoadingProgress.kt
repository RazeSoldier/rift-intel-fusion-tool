package dev.nohus.rift.assets.compose

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
import dev.nohus.rift.assets.AssetsRepository
import dev.nohus.rift.assets.AssetsRepository.AssetOwner
import dev.nohus.rift.assets.AssetsRepository.LoadingStage
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.InfiniteScrollingCarousel
import dev.nohus.rift.compose.LoadingBox
import dev.nohus.rift.compose.LoadingSpinnerAmbient
import dev.nohus.rift.compose.ScrollbarColumn
import dev.nohus.rift.compose.VerticalGrid
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax

@Composable
fun AssetsLoadingProgress(
    loading: AssetsRepository.LoadingState,
    stage: LoadingStage
) {
    ScrollbarColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(Spacing.large),
    ) {
        LoadingSpinnerAmbient()
        Spacer(Modifier.height(Spacing.medium))
        when (stage) {
            LoadingStage.LoadingAssets -> {
                Text(
                    text = "Loading assets…",
                    style = RiftTheme.typography.headlinePrimary,
                )
            }
            LoadingStage.LoadingLocations -> {
                Text(
                    text = "Loading asset locations…",
                    style = RiftTheme.typography.headlinePrimary,
                )
            }
            LoadingStage.LoadingDivisionNames -> {
                Text(
                    text = "Loading division names…",
                    style = RiftTheme.typography.headlinePrimary,
                )
            }
        }

        Spacer(Modifier.height(Spacing.large))

        VerticalGrid(
            minColumnWidth = 300.dp,
            horizontalSpacing = Spacing.medium,
            verticalSpacing = Spacing.medium,
        ) {
            if (stage == LoadingStage.LoadingLocations) {
                val percentageStations = loading.loadedStationIds.toFloat() / (loading.totalStationIds ?: 1)
                LoadingBox(percentageStations) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val stationTypeIds = listOf(1929, 1529, 3867, 2502)
                        InfiniteScrollingCarousel(
                            items = stationTypeIds,
                            delay = 1_000,
                            modifier = Modifier.size(48.dp),
                        ) { typeId ->
                            AsyncTypeIcon(
                                typeId = typeId,
                                modifier = Modifier.size(48.dp),
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            Text(
                                text = "Stations",
                                style = RiftTheme.typography.bodyPrimary,
                            )

                            Divider(color = Color.White.copy(alpha = 0.15f))

                            if (loading.loadedStationIds == loading.totalStationIds) {
                                Text(
                                    text = "${loading.loadedStationIds} stations loaded",
                                    style = RiftTheme.typography.bodySecondary,
                                )
                            } else {
                                Text(
                                    text = "${loading.loadedStationIds} stations loaded…",
                                    style = RiftTheme.typography.bodyPrimary,
                                )
                            }
                        }
                    }
                }

                val percentageStructures = loading.loadedStructureIds.toFloat() / (loading.totalStructureIds ?: 1)
                LoadingBox(percentageStructures) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val structureTypeIds = listOf(35834, 35833, 35832)
                        InfiniteScrollingCarousel(
                            items = structureTypeIds,
                            delay = 1_000,
                            modifier = Modifier.size(48.dp),
                        ) { typeId ->
                            AsyncTypeIcon(
                                typeId = typeId,
                                modifier = Modifier.size(48.dp),
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            Text(
                                text = "Structures",
                                style = RiftTheme.typography.bodyPrimary,
                            )

                            Divider(color = Color.White.copy(alpha = 0.15f))

                            Text(
                                text = "${loading.loadedStructureIds} structures loaded…",
                                style = RiftTheme.typography.bodyPrimary,
                            )
                        }
                    }
                }
            }

            loading.owners.sortedBy { it is AssetOwner.Corporation }.forEach { owner ->
                val progress = loading.ownerProgress[owner]
                val assets = progress?.first ?: 0
                val percentage = progress?.second ?: 0f
                val isNamesLoaded = owner in loading.ownerNamesLoaded

                LoadingBox(percentage) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        when (owner) {
                            is AssetOwner.Character -> {
                                DynamicCharacterPortraitParallax(
                                    characterId = owner.character.characterId,
                                    size = 48.dp,
                                    enterTimestamp = null,
                                    pointerInteractionStateHolder = null,
                                )
                            }

                            is AssetOwner.Corporation -> {
                                AsyncCorporationLogo(
                                    corporationId = owner.corporationId,
                                    size = 64,
                                    modifier = Modifier.size(48.dp),
                                )
                            }
                        }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            Text(
                                text = owner.character.info?.name ?: "Unknown",
                                style = RiftTheme.typography.bodyPrimary,
                            )

                            Divider(color = Color.White.copy(alpha = 0.15f))

                            if (isNamesLoaded) {
                                Text(
                                    text = "$assets assets loaded",
                                    style = RiftTheme.typography.bodySecondary,
                                )
                            } else if (percentage >= 1f) {
                                Text(
                                    text = "$assets assets loaded, loading names…",
                                    style = RiftTheme.typography.bodyPrimary,
                                )
                            } else {
                                Text(
                                    text = "$assets assets loaded…",
                                    style = RiftTheme.typography.bodyPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
