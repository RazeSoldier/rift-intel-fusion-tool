package dev.nohus.rift.structures.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.EntityInteractionProvider
import dev.nohus.rift.compose.RiftCircularProgressGauge
import dev.nohus.rift.compose.RiftIndicatorDot
import dev.nohus.rift.compose.RiftProgressBar
import dev.nohus.rift.compose.RiftSolarSystemChip
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.greyScale
import dev.nohus.rift.compose.modifyIf
import dev.nohus.rift.compose.text.FormattedTextColor
import dev.nohus.rift.compose.text.Link
import dev.nohus.rift.compose.text.LinkStyle
import dev.nohus.rift.compose.text.LinkedText
import dev.nohus.rift.compose.text.buildFormattedText
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.di.koin
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.arrow_down_16px
import dev.nohus.rift.generated.resources.arrow_right_16px
import dev.nohus.rift.generated.resources.arrow_up_16px
import dev.nohus.rift.generated.resources.magmatic_gas_16px
import dev.nohus.rift.generated.resources.power_16px
import dev.nohus.rift.generated.resources.superionic_ice_16px
import dev.nohus.rift.generated.resources.workforce_16px
import dev.nohus.rift.generated.resources.workforce_32px
import dev.nohus.rift.network.esi.models.SovereigntyHubPower
import dev.nohus.rift.network.esi.models.SovereigntyHubUpgradePowerState
import dev.nohus.rift.network.esi.models.SovereigntyHubWorkforce
import dev.nohus.rift.structures.SovereigntyReagent
import dev.nohus.rift.structures.StructuresRepository.SovereigntyHub
import dev.nohus.rift.structures.StructuresRepository.SovereigntyHubReagent
import dev.nohus.rift.structures.StructuresRepository.SovereigntyHubWorkforceTransport
import dev.nohus.rift.utils.formatDateTime
import dev.nohus.rift.utils.formatNumber
import dev.nohus.rift.utils.multiplyBrightness
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.withColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun SovereigntyHub(
    sovereigntyHub: SovereigntyHub,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        SovereigntyHubTitleRow(sovereigntyHub)
        Spacer(Modifier.height(Spacing.mediumLarge))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Column {
                Row {
                    sovereigntyHub.reagentBay.forEach { reagent ->
                        ReagentGauge(reagent, sovereigntyHub, now)
                    }
                }
                Spacer(Modifier.height(Spacing.mediumLarge))
                SovereigntyHubUpgrades(sovereigntyHub)
            }
            Column {
                SovereigntyHubPower(sovereigntyHub.resources.power)
                Spacer(Modifier.height(Spacing.mediumLarge))
                SovereigntyHubWorkforce(sovereigntyHub.resources.workforce, sovereigntyHub.workforceTransportConfiguration, sovereigntyHub.workforceTransportState)
                Spacer(Modifier.height(Spacing.mediumLarge))
                Vulnerability("Vulnerability", sovereigntyHub.vulnerabilityWindow, now)
            }
        }
    }
}

@Composable
private fun SovereigntyHubUpgrades(sovereigntyHub: SovereigntyHub) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        sovereigntyHub.upgrades.forEach { upgrade ->
            val (name, color, isActive) = when (upgrade.powerState) {
                SovereigntyHubUpgradePowerState.Online -> Triple("Online", EveColors.successGreen, true)
                SovereigntyHubUpgradePowerState.Low -> Triple("Low", EveColors.warningOrange, true)
                SovereigntyHubUpgradePowerState.Pending -> Triple("Pending", EveColors.warningOrange, true)
                SovereigntyHubUpgradePowerState.Offline -> Triple("Offline", RiftTheme.colors.textSecondary, false)
                SovereigntyHubUpgradePowerState.Unspecified -> Triple("Unknown state", RiftTheme.colors.textSecondary, false)
            }
            val textColor = if (upgrade.powerState == SovereigntyHubUpgradePowerState.Online) {
                RiftTheme.colors.textPrimary
            } else {
                RiftTheme.colors.textSecondary
            }

            RiftTooltipArea(
                tooltip = @Composable {
                    Column(
                        modifier = Modifier.padding(Spacing.large),
                    ) {
                        Text(
                            text = upgrade.type.name,
                            style = RiftTheme.typography.bodyPrimary,
                        )
                        Text(
                            text = name,
                            style = RiftTheme.typography.bodyPrimary.copy(color = color),
                        )
                        Spacer(Modifier.height(Spacing.small))
                        if (upgrade.details.powerConsumption > 0) {
                            Text(
                                text = "Power Allocation: ${formatNumber(upgrade.details.powerConsumption)}",
                                style = RiftTheme.typography.detailPrimary,
                            )
                        } else {
                            Text(
                                text = "Power Production: ${formatNumber(upgrade.details.powerConsumption.absoluteValue)}",
                                style = RiftTheme.typography.detailPrimary,
                            )
                        }
                        if (upgrade.details.workforceConsumption > 0) {
                            Text(
                                text = "Workforce Allocation: ${formatNumber(upgrade.details.workforceConsumption)}",
                                style = RiftTheme.typography.detailPrimary,
                            )
                        } else {
                            Text(
                                text = "Workforce Production: ${formatNumber(upgrade.details.workforceConsumption.absoluteValue)}",
                                style = RiftTheme.typography.detailPrimary,
                            )
                        }
                        when (upgrade.details.fuel?.type) {
                            SovereigntyReagent.SuperionicIce -> {
                                Text(
                                    text = "Fuel: Superionic Ice",
                                    style = RiftTheme.typography.detailPrimary,
                                )
                            }
                            SovereigntyReagent.MagmaticGas -> {
                                Text(
                                    text = "Fuel: Magmatic Gas",
                                    style = RiftTheme.typography.detailPrimary,
                                )
                            }
                            null -> {}
                        }
                        if (upgrade.details.fuel != null) {
                            Text(
                                text = "Fuel consumption: ${formatNumber(upgrade.details.fuel.hourlyUpkeep)}/h",
                                style = RiftTheme.typography.detailPrimary,
                            )
                            Text(
                                text = "Fuel startup cost: ${formatNumber(upgrade.details.fuel.startupCost)}",
                                style = RiftTheme.typography.detailPrimary,
                            )
                        }
                    }
                },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncTypeIcon(
                        type = upgrade.type,
                        modifier = Modifier
                            .modifyIf(upgrade.powerState != SovereigntyHubUpgradePowerState.Online) {
                                greyScale()
                            }
                            .size(32.dp),
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            RiftIndicatorDot(
                                color = color,
                                isActive = isActive,
                            )
                            Text(
                                text = upgrade.type.name,
                                style = RiftTheme.typography.bodyPrimary.copy(color = textColor),
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            SovereigntyHubUpgradesResourceCost(
                                icon = Res.drawable.power_16px,
                                consumption = upgrade.details.powerConsumption,
                                textColor = textColor,
                            )
                            SovereigntyHubUpgradesResourceCost(
                                icon = Res.drawable.workforce_16px,
                                consumption = upgrade.details.workforceConsumption,
                                textColor = textColor,
                            )
                            if (upgrade.details.fuel != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val icon = when (upgrade.details.fuel.type) {
                                        SovereigntyReagent.SuperionicIce -> Res.drawable.superionic_ice_16px
                                        SovereigntyReagent.MagmaticGas -> Res.drawable.magmatic_gas_16px
                                    }
                                    Image(
                                        painter = painterResource(icon),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(textColor),
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Text(
                                        text = "${upgrade.details.fuel.hourlyUpkeep}/h",
                                        style = RiftTheme.typography.detailPrimary.copy(color = textColor),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SovereigntyHubUpgradesResourceCost(
    icon: DrawableResource,
    consumption: Int,
    textColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (consumption < 0) EveColors.leafyGreen else textColor
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = "${if (consumption < 0) "+" else ""}${formatNumber(consumption.absoluteValue)}",
            style = RiftTheme.typography.detailPrimary.copy(color = color),
        )
    }
}

@Composable
private fun SovereigntyHubTitleRow(
    sovereigntyHub: SovereigntyHub,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Brush.linearGradient(listOf(Color.Transparent, RiftTheme.colors.backgroundPrimary)))
            .padding(Spacing.small),
    ) {
        Column {
            val text = buildAnnotatedString {
                append("Sovereignty Hub – ")
                append(sovereigntyHub.system.name)
            }
            Text(
                text = text,
                style = RiftTheme.typography.bodyPrimary,
            )
            Spacer(Modifier.height(Spacing.small))
            RiftSolarSystemChip(
                state = sovereigntyHub.systemChip,
            )
        }
        Spacer(Modifier.weight(1f))
        StructureOwner(sovereigntyHub.character)
    }
}

@Composable
private fun ReagentGauge(
    reagent: SovereigntyHubReagent,
    sovereigntyHub: SovereigntyHub,
    now: Instant,
) {
    val timePassed = Duration.between(sovereigntyHub.reagentBayLastUpdated, now)
    val burningPerSecond = reagent.burningPerHour / 3600f
    val amountNow = (reagent.amount - burningPerSecond * timePassed.toSeconds()).toInt().coerceAtLeast(0)

    val hoursRemaining = if (reagent.amount > 0 && reagent.burningPerHour > 0) reagent.amount / reagent.burningPerHour else 0
    val depletesAt = sovereigntyHub.reagentBayLastUpdated + Duration.ofHours(hoursRemaining.toLong())

    val timeLeft = Duration.between(now, depletesAt)
    RiftTooltipArea(
        text = if (timeLeft.isPositive) {
            "${reagent.name} depletes\n${formatDateTime(depletesAt)}"
        } else {
            "No ${reagent.name} available"
        },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RiftCircularProgressGauge(
                progress = timeLeft.toSeconds().toFloat() / Duration.ofDays(28).toSeconds(),
                trackColor = EveColors.cryoBlue.multiplyBrightness(0.4f),
                color = EveColors.cryoBlue,
                diameter = 125.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(reagent.icon),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = formatReagentTimeLeft(timeLeft),
                        style = RiftTheme.typography.headlineHighlighted,
                    )
                    Text(
                        text = formatNumber(amountNow),
                        style = RiftTheme.typography.bodySecondary,
                    )
                    Text(
                        text = "${reagent.burningPerHour}/h",
                        style = RiftTheme.typography.bodySecondary,
                    )
                }
            }
            val isUsed = sovereigntyHub.upgrades
                .filter { it.powerState != SovereigntyHubUpgradePowerState.Offline }
                .any {
                    it.details.fuel?.type == reagent.type && it.details.fuel.hourlyUpkeep > 0
                }
            val (statusText, color) = when {
                timeLeft.toDays() >= 28 -> "Fuel Full" to RiftTheme.colors.textSecondary
                timeLeft.toDays() >= 7 -> "Fuel Low" to EveColors.warningOrange
                timeLeft.isPositive -> "Fuel Critical" to EveColors.dangerRed
                else -> if (isUsed) {
                    "Depleted" to EveColors.dangerRed
                } else {
                    "Unused" to RiftTheme.colors.textSecondary
                }
            }
            Text(
                text = statusText,
                style = RiftTheme.typography.detailSecondary.copy(color = color, fontWeight = FontWeight.Bold),
            )
        }
    }
}

private fun formatReagentTimeLeft(timeLeft: Duration): String {
    return when {
        timeLeft.isNegative -> "–"
        timeLeft.toDays() >= 28 -> "${timeLeft.toDays() / 7} weeks"
        timeLeft.toDays() >= 2 -> "${timeLeft.toDays()} days"
        timeLeft.toHours() >= 24 -> "${timeLeft.toHours()} hours"
        timeLeft.toHours() >= 1 -> String.format(Locale.ENGLISH, "%d:%02d %s", timeLeft.toHours(), timeLeft.toMinutesPart(), "hour${timeLeft.toHours().plural}")
        else -> String.format(Locale.ENGLISH, "%d:%02d %s", timeLeft.toMinutes(), timeLeft.toSecondsPart(), "minutes${timeLeft.toMinutes().plural}")
    }
}

@Composable
private fun SovereigntyHubPower(power: SovereigntyHubPower) {
    Column(
        modifier = Modifier.width(IntrinsicSize.Max),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Power",
                    style = RiftTheme.typography.detailSecondary,
                )
                Text(
                    text = buildAnnotatedString {
                        withColor(RiftTheme.colors.textPrimary) {
                            append(formatNumber(power.allocated))
                        }
                        append(" / ")
                        append(formatNumber(power.available))
                    },
                    style = RiftTheme.typography.bodySecondary,
                )
            }
            Text(
                text = formatNumber(power.available),
                style = RiftTheme.typography.bodySecondary,
            )
        }
        Spacer(Modifier.height(Spacing.small))
        val unallocatedPower = power.available - power.allocated
        RiftProgressBar(
            percentage = power.allocated / power.available.toFloat(),
            secondaryPercentage = unallocatedPower / power.available.toFloat(),
            color = EveColors.smokeBlue,
            secondaryColor = EveColors.cryoBlue,
            modifier = Modifier.size(300.dp, 4.dp),
        )
    }
}

@Composable
private fun SovereigntyHubWorkforce(
    workforce: SovereigntyHubWorkforce,
    configuration: SovereigntyHubWorkforceTransport,
    state: SovereigntyHubWorkforceTransport,
) {
    Column(
        modifier = Modifier.width(300.dp),
    ) {
        val exported = (state as? SovereigntyHubWorkforceTransport.Export)?.amount ?: 0
        val total = workforce.available + exported

        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Workforce",
                    style = RiftTheme.typography.detailSecondary,
                )
                Text(
                    text = buildAnnotatedString {
                        withColor(RiftTheme.colors.textPrimary) {
                            append(formatNumber(workforce.allocated))
                        }
                        append(" / ")
                        append(formatNumber(workforce.available))
                    },
                    style = RiftTheme.typography.bodySecondary,
                )
            }
            Text(
                text = formatNumber(total),
                style = RiftTheme.typography.bodySecondary,
            )
        }
        Spacer(Modifier.height(Spacing.small))

        RiftProgressBar(
            percentage = workforce.allocated / total.toFloat(),
            secondaryPercentage = (workforce.available - workforce.allocated) / total.toFloat(),
            tertiaryPercentage = exported / total.toFloat(),
            color = EveColors.smokeBlue,
            secondaryColor = EveColors.cryoBlue,
            tertiaryColor = EveColors.copperOxideGreen,
            modifier = Modifier.height(4.dp),
        )
        Spacer(Modifier.height(Spacing.mediumLarge))

        SovereigntyHubWorkforceTransportStatus(state)
        if (!state.isMatching(configuration)) {
            Spacer(Modifier.height(Spacing.mediumLarge))
            SovereigntyHubWorkforceTransportStatus(configuration, isPending = true)
        }
    }
}

@Composable
private fun SovereigntyHubWorkforceTransportStatus(
    transport: SovereigntyHubWorkforceTransport,
    isPending: Boolean = false,
) {
    Column {
        Text(
            text = if (isPending) "Pending Workforce Transport Configuration" else "Workforce Transport",
            style = RiftTheme.typography.detailSecondary,
        )
        val (mode, icon) = when (transport) {
            is SovereigntyHubWorkforceTransport.Export -> "Export" to Res.drawable.arrow_up_16px
            is SovereigntyHubWorkforceTransport.Import -> "Import" to Res.drawable.arrow_down_16px
            SovereigntyHubWorkforceTransport.Transit -> "Transit" to Res.drawable.arrow_right_16px
            SovereigntyHubWorkforceTransport.Idle -> "Idle" to Res.drawable.workforce_32px
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = mode,
                style = RiftTheme.typography.headlinePrimary,
            )
        }
        val interactionProvider: EntityInteractionProvider = remember { koin.get() }
        when (transport) {
            is SovereigntyHubWorkforceTransport.Export -> {
                val text = buildFormattedText {
                    append("This sovereignty hub ${if (isPending) "will be" else "is"} exporting ")
                    withColor(FormattedTextColor.Primary) {
                        append(formatNumber(transport.amount))
                    }
                    append(" workforce unit${transport.amount.plural} to ")
                    if (transport.solarSystem != null) {
                        val interaction = interactionProvider.getLocation(transport.solarSystem.id)
                        val link = Link(
                            style = LinkStyle.HoverUnderline,
                            onClick = interaction.onClick,
                            contextMenuItems = interaction.contextMenuItems,
                        )
                        withLink(link) {
                            withWeight(FontWeight.Bold) {
                                withColor(FormattedTextColor.Primary) {
                                    append(transport.solarSystem.name)
                                }
                            }
                        }
                    }
                    append(".")
                }
                LinkedText(
                    text = text,
                    style = RiftTheme.typography.bodySecondary,
                )
            }
            is SovereigntyHubWorkforceTransport.Import -> {
                val text = buildFormattedText {
                    append("This sovereignty hub ${if (isPending) "will be" else "is"} importing ")
                    if (isPending) {
                        append("workforce from: ")
                        transport.sources.forEachIndexed { index, source ->
                            if (index > 0) append(", ")
                            if (source.solarSystem != null) {
                                val interaction = interactionProvider.getLocation(source.solarSystem.id)
                                val link = Link(
                                    style = LinkStyle.HoverUnderline,
                                    onClick = interaction.onClick,
                                    contextMenuItems = interaction.contextMenuItems,
                                )
                                withLink(link) {
                                    withWeight(FontWeight.Bold) {
                                        withColor(FormattedTextColor.Primary) {
                                            append(source.solarSystem.name)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        transport.sources.forEachIndexed { index, source ->
                            if (index > 0) append(", ")
                            withColor(FormattedTextColor.Primary) {
                                append(formatNumber(source.amount))
                            }
                            append(" workforce unit${0.plural} from ")
                            if (source.solarSystem != null) {
                                val interaction = interactionProvider.getLocation(source.solarSystem.id)
                                val link = Link(
                                    style = LinkStyle.HoverUnderline,
                                    onClick = interaction.onClick,
                                    contextMenuItems = interaction.contextMenuItems,
                                )
                                withLink(link) {
                                    withWeight(FontWeight.Bold) {
                                        withColor(FormattedTextColor.Primary) {
                                            append(source.solarSystem.name)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    append(".")
                }
                LinkedText(
                    text = text,
                    style = RiftTheme.typography.bodySecondary,
                )
            }
            SovereigntyHubWorkforceTransport.Transit -> {
                Text(
                    text = "This sovereignty hub ${if (isPending) "will be" else "is"} set to transit workforce between systems.",
                    style = RiftTheme.typography.bodySecondary,
                )
            }
            SovereigntyHubWorkforceTransport.Idle -> {
                Text(
                    text = "This sovereignty hub ${if (isPending) "will not be" else "is not"} transporting workforce between systems.",
                    style = RiftTheme.typography.bodySecondary,
                )
            }
        }
    }
}
