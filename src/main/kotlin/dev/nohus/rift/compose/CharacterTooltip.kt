package dev.nohus.rift.compose

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.score_background
import dev.nohus.rift.repositories.FactionsRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import org.jetbrains.compose.resources.painterResource
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CharacterTooltip(
    character: CharacterDetailsRepository.CharacterDetails?,
    content: @Composable () -> Unit,
) {
    if (character != null) {
        RiftTooltipArea(
            tooltip = {
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
                            FlagIcon(character.standing)
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

                        if (character.dangerRatio != null) {
                            LinkedText(
                                text = buildFormattedText {
                                    append("Danger: ")
                                    withColor(FormattedTextColor.Primary) {
                                        append("${character.dangerRatio}%")
                                    }
                                },
                                style = RiftTheme.typography.bodySecondary,
                            )
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
