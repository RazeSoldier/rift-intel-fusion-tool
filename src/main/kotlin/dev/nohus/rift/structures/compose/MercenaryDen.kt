package dev.nohus.rift.structures.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.ClickableAlliance
import dev.nohus.rift.compose.ClickableCharacter
import dev.nohus.rift.compose.ClickableCorporation
import dev.nohus.rift.compose.RiftButton
import dev.nohus.rift.compose.RiftCircularProgressGauge
import dev.nohus.rift.compose.RiftIndicatorDot
import dev.nohus.rift.compose.RiftSolarSystemChip
import dev.nohus.rift.compose.RiftStatsRow
import dev.nohus.rift.compose.RiftStatsRowItem
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.circle
import dev.nohus.rift.generated.resources.upwell_reinforced_armor
import dev.nohus.rift.generated.resources.upwell_reinforced_hull
import dev.nohus.rift.generated.resources.upwell_vulnerable_armor
import dev.nohus.rift.generated.resources.upwell_vulnerable_hull
import dev.nohus.rift.network.esi.models.MercenaryDenState
import dev.nohus.rift.network.esi.models.SkyhookState
import dev.nohus.rift.opportunities.MercenaryTacticalOperationsRepository
import dev.nohus.rift.opportunities.MercenaryTacticalOperationsRepository.MercenaryTacticalOperation
import dev.nohus.rift.standings.getColor
import dev.nohus.rift.structures.StructuresRepository.MercenaryDen
import dev.nohus.rift.utils.formatDurationCompact
import dev.nohus.rift.utils.formatNumber
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant

@Composable
fun MercenaryDen(
    mercenaryDen: MercenaryDen,
    operations: List<MercenaryTacticalOperation>,
    now: Instant,
    onViewOperationClick: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        MercenaryDenTitleRow(mercenaryDen)
        Spacer(Modifier.height(Spacing.large))

        val translationX = LocalDensity.current.run { 7.dp.toPx() }
        RiftStatsRow(
            modifier.graphicsLayer(translationX = -translationX),
        ) {
            RiftStatsRowItem(
                value = formatNumber(mercenaryDen.infomorphs),
                text = "Infomorph Decryption Keys",
                color = EveColors.airTurquoise,
                valueStyle = RiftTheme.typography.headlinePrimary,
            )
            val multiplier = when (mercenaryDen.evolution.developmentLevel) {
                1 -> 1.15
                2 -> 1.25
                3 -> 1.35
                4 -> 1.5
                else -> 1.0
            }
            val rangeFrom = (52 * multiplier).toInt()
            val rangeTo = (80 * multiplier).toInt()
            RiftStatsRowItem(
                value = "$rangeFrom-$rangeTo/h",
                text = "Infomorph Decryption Key Rate",
                color = EveColors.airTurquoise,
                valueStyle = RiftTheme.typography.headlinePrimary,
            )
            val workforceImpact = when (mercenaryDen.evolution.anarchyLevel) {
                2 -> "10%"
                3 -> "20%"
                4 -> "40%"
                else -> "0%"
            }
            RiftStatsRowItem(
                value = workforceImpact,
                text = "Workforce Impact",
                color = EveColors.airTurquoise,
                valueStyle = RiftTheme.typography.headlinePrimary,
            )
        }
        Spacer(Modifier.height(Spacing.large))

        Row {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.large),
            ) {
                EvolutionTrack("Development", mercenaryDen.evolution.developmentLevel, mercenaryDen.evolution.developmentAmount)
                EvolutionTrack("Anarchy", mercenaryDen.evolution.anarchyLevel, mercenaryDen.evolution.anarchyAmount)

                if (mercenaryDen.reinforcementTimer != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.upwell_reinforced_armor),
                            contentDescription = null,
                            modifier = Modifier.height(32.dp)
                        )
                        val reinforcedUntil = Duration.between(now, mercenaryDen.reinforcementTimer.end)
                        Text(
                            text = if (reinforcedUntil.isPositive) {
                                formatDurationCompact(reinforcedUntil)
                            } else "Reinforcement ended",
                            style = RiftTheme.typography.bodyPrimary,
                        )
                    }
                }
            }
            Spacer(Modifier.width(Spacing.large))
            Column {
                SkyhookOwner(mercenaryDen)
                Spacer(Modifier.height(Spacing.mediumLarge))
                MercenaryTacticalOperations(operations, onViewOperationClick)
            }
        }
    }
}

@Composable
private fun StateIndicator(mercenaryDen: MercenaryDen) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        val (state, color) = when (mercenaryDen.state) {
            MercenaryDenState.Running -> "Active" to EveColors.successGreen
            MercenaryDenState.Paused -> "Paused" to EveColors.dangerRed
            MercenaryDenState.Disabled -> "Disabled" to EveColors.dangerRed
            MercenaryDenState.Unspecified -> "Unknown" to EveColors.dangerRed
        }
        RiftIndicatorDot(
            color = color,
            isActive = mercenaryDen.state == MercenaryDenState.Running,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Text(
            text = state,
            style = RiftTheme.typography.detailPrimary,
        )
    }
}

@Composable
private fun SkyhookOwner(mercenaryDen: MercenaryDen) {
    Column {
        Text(
            text = "Skyhook Owner",
            style = RiftTheme.typography.headlinePrimary,
        )
        Spacer(Modifier.height(Spacing.medium))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClickableCorporation(mercenaryDen.skyhookCorporationId) {
                AsyncCorporationLogo(
                    corporationId = mercenaryDen.skyhookCorporationId,
                    size = 64,
                    modifier = Modifier
                        .size(32.dp),
                )
            }
            Column {
                val color = mercenaryDen.skyhookCorporation?.standingLevel?.getColor() ?: RiftTheme.colors.textPrimary
                ClickableCorporation(mercenaryDen.skyhookCorporationId) {
                    Text(
                        text = mercenaryDen.skyhookCorporation?.corporationName ?: "",
                        style = RiftTheme.typography.bodyPrimary.copy(color = color, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun MercenaryTacticalOperations(
    operations: List<MercenaryTacticalOperation>,
    onViewOperationClick: (String) -> Unit,
) {
    Column {
        Text(
            text = "Mercenary Tactical Operations",
            style = RiftTheme.typography.headlinePrimary,
        )
        Spacer(Modifier.height(Spacing.medium))

        if (operations.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                operations.forEach {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.widthIn(min = 200.dp),
                        ) {
                            Text(
                                text = it.type?.name ?: "Unknown",
                                style = RiftTheme.typography.bodyPrimary.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = it.state.name,
                                style = RiftTheme.typography.bodySecondary,
                            )
                        }
                        Spacer(Modifier.width(Spacing.large))
                        RiftButton(
                            text = "View Details",
                            onClick = {
                                onViewOperationClick(it.id)
                            },
                        )
                    }
                }
            }
        } else {
            Text(
                text = "No operations currently available",
                style = RiftTheme.typography.bodyPrimary,
            )
        }
    }
}

@Composable
private fun EvolutionTrack(title: String, level: Int, amount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.width(260.dp),
    ) {
        Column {
            Text(
                text = title,
                style = RiftTheme.typography.headlinePrimary,
            )
            Text(
                text = "Level $level",
                style = RiftTheme.typography.displayHighlighted,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            (1..4).forEach {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(30.dp),
                ) {
                    val amountRange = when (it) {
                        1 -> 0..19
                        2 -> 20..39
                        3 -> 40..69
                        4 -> 70..99
                        else -> 100..100
                    }
                    val amountInLevel = amount - amountRange.first
                    val amountTotalInLevel = amountRange.last + 1 - amountRange.first
                    val isComplete = it <= level
                    if (isComplete) {
                        RiftTooltipArea(
                            text = """
                                $title Progress:
                                Level $it ($amountTotalInLevel/$amountTotalInLevel)
                                ${getEvolutionBonus(title, it)}
                            """.trimIndent(),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.circle),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(EveColors.cryoBlue),
                                    modifier = Modifier.size(24.dp),
                                )
                                Text(
                                    text = "$it",
                                    style = RiftTheme.typography.detailHighlighted,
                                )
                            }
                        }
                    } else if (it - 1 == level) {
                        RiftTooltipArea(
                            text = """
                                $title Progress:
                                Level $it ($amountInLevel/$amountTotalInLevel)
                                ${getEvolutionBonus(title, it)}
                            """.trimIndent(),
                        ) {
                            val progress = amountInLevel / amountTotalInLevel.toFloat()
                            RiftCircularProgressGauge(
                                progress = progress,
                                trackColor = EveColors.gunmetalGrey,
                                color = EveColors.cryoBlue,
                                diameter = 30.dp,
                                gaugeWidth = 10.dp,
                            ) {
                                Text(
                                    text = "$it",
                                    style = RiftTheme.typography.detailHighlighted,
                                )
                            }
                        }
                    } else {
                        RiftTooltipArea(
                            text = """
                                $title Progress:
                                Level $it (0/$amountTotalInLevel)
                                ${getEvolutionBonus(title, it)}
                            """.trimIndent(),
                        ) {
                            RiftCircularProgressGauge(
                                progress = 0f,
                                trackColor = EveColors.gunmetalGrey.copy(alpha = 0.3f),
                                diameter = 30.dp,
                                gaugeWidth = 10.dp,
                            ) {
                                Text(
                                    text = "$it",
                                    style = RiftTheme.typography.detailSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getEvolutionBonus(type: String, level: Int): String {
    return if (type.lowercase() == "development") {
        when (level) {
            1 -> "15% Infomorph Production Bonus"
            2 -> "25% Infomorph Production Bonus"
            3 -> "35% Infomorph Production Bonus"
            4 -> "50% Infomorph Production Bonus"
            else -> "Regular Infomorph Production Rate"
        }
    } else {
        when (level) {
            2 -> "10% Workforce Disrupted"
            3 -> "20% Workforce Disrupted"
            4 -> "40% Workforce Disrupted"
            else -> "No Workforce Disruption"
        }
    }
}

@Composable
private fun MercenaryDenTitleRow(
    mercenaryDen: MercenaryDen,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Brush.linearGradient(listOf(Color.Transparent, RiftTheme.colors.backgroundPrimary)))
            .padding(Spacing.small),
    ) {
        Column {
            val text = buildAnnotatedString {
                append("Mercenary Den – ")
                append(mercenaryDen.planet.name)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.large),
            ) {
                Text(
                    text = text,
                    style = RiftTheme.typography.bodyPrimary,
                )
                StateIndicator(mercenaryDen)
            }
            Spacer(Modifier.height(Spacing.small))
            RiftSolarSystemChip(
                state = mercenaryDen.systemChip,
            )
        }
        Spacer(Modifier.weight(1f))
        StructureOwner(mercenaryDen.character, isShowingCharacter = true)
    }
}
