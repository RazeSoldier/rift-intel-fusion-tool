package dev.nohus.rift.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.text.FormattedTextColor
import dev.nohus.rift.compose.text.LinkedText
import dev.nohus.rift.compose.text.ParseEveFormattedTextUseCase
import dev.nohus.rift.compose.text.buildFormattedText
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.network.zkillboard.ZkillCharacterStats
import dev.nohus.rift.network.zkillboard.ZkillboardApi
import dev.nohus.rift.repositories.FactionsRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CharacterTooltip(
    character: CharacterDetailsRepository.CharacterDetails?,
    content: @Composable () -> Unit,
) {
    if (character != null) {
        RiftTooltipArea(
            tooltip = {
                val zkillboardApi: ZkillboardApi = remember { koin.get() }
                var zkillStats: Result<ZkillCharacterStats>? by remember(character.characterId) { mutableStateOf(null) }
                var iszKillLoadingShown by remember(character.characterId) { mutableStateOf(false) }
                LaunchedEffect(character.characterId) {
                    launch {
                        delay(100.milliseconds)
                        iszKillLoadingShown = true
                    }
                    launch {
                        zkillStats = zkillboardApi.getCharacterStats(Originator.Killmails, character.characterId)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                    modifier = Modifier.padding(Spacing.medium),
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            Text(
                                text = character.name,
                                style = RiftTheme.typography.headlineHighlighted,
                            )
                            FlagIcon(character.standingLevel)
                        }
                        if (character.title != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                            ) {
                                AchievementText(character.title, RiftTheme.typography.detailHighlighted)
                                AchievementText(character.achievementScore.toString(), RiftTheme.typography.detailPrimary)
                            }
                        }
                        Spacer(Modifier.height(Spacing.small))
                        if (character.corporationTitle != null) {
                            val parseEveFormattedTextUseCase: ParseEveFormattedTextUseCase = remember { koin.get() }
                            val formatted = parseEveFormattedTextUseCase(character.corporationTitle)
                            LinkedText(
                                text = buildFormattedText {
                                    append("Title: ")
                                    withColor(FormattedTextColor.Primary) {
                                        append(formatted)
                                    }
                                },
                                style = RiftTheme.typography.bodySecondary,
                                modifier = Modifier.widthIn(max = 250.dp),
                            )
                        }

                        if (character.factionId != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncCorporationLogo(
                                    corporationId = character.factionId,
                                    size = 32,
                                    modifier = Modifier.size(32.dp),
                                )
                                val factionsRepository: FactionsRepository = remember { koin.get() }
                                Text(
                                    text = "${factionsRepository.getFaction(character.factionId)}",
                                )
                            }
                        }

                        LinkedText(
                            text = buildFormattedText {
                                append("Security Status: ")
                                withColor(FormattedTextColor.Primary) {
                                    append(String.format("%.1f", character.securityStatus))
                                }
                            },
                            style = RiftTheme.typography.bodySecondary,
                        )

                        Row {
                            Text(
                                text = "Danger: ",
                                style = RiftTheme.typography.bodySecondary,
                            )
                            zkillStats?.let { zkillStats ->
                                val text = when (zkillStats) {
                                    is Result.Success if zkillStats.data.dangerRatio != null -> "${zkillStats.data.dangerRatio}%"
                                    else -> "Unknown"
                                }
                                Text(
                                    text = text,
                                    style = RiftTheme.typography.bodyPrimary,
                                )
                            } ?: run {
                                if (iszKillLoadingShown) {
                                    LoadingSpinner16()
                                }
                            }
                        }

                        val time = ZonedDateTime.ofInstant(character.birthday, ZoneId.of("UTC"))
                        val formatted = DateTimeFormatter.ofPattern("yyyy.MM.dd").format(time)
                        LinkedText(
                            text = buildFormattedText {
                                append("Created: ")
                                withColor(FormattedTextColor.Primary) {
                                    append(formatted)
                                }
                            },
                            style = RiftTheme.typography.bodySecondary,
                        )
                    }
                }
            },
            content = content,
        )
    } else {
        content()
    }
}

@Composable
private fun AchievementText(text: String, style: TextStyle) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = EveColors.white.copy(alpha = 0.4f),
                shape = RoundedCornerShape(Spacing.small),
            )
            .background(
                color = EveColors.white.copy(alpha = 0.2f),
                shape = RoundedCornerShape(Spacing.small),
            )
            .padding(
                horizontal = Spacing.small,
                vertical = Spacing.verySmall,
            ),
    ) {
        Text(
            text = text,
            style = style,
        )
    }
}
