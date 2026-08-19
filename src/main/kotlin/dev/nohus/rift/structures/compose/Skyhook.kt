package dev.nohus.rift.structures.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.nohus.rift.compose.RiftSolarSystemChip
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.mercenary_den_32px
import dev.nohus.rift.generated.resources.mercenary_den_laser_32px
import dev.nohus.rift.generated.resources.pi_disc_shadow
import dev.nohus.rift.generated.resources.power_32px
import dev.nohus.rift.generated.resources.skyhook_illustration_container
import dev.nohus.rift.generated.resources.skyhook_illustration_elevator
import dev.nohus.rift.generated.resources.skyhook_illustration_structure
import dev.nohus.rift.generated.resources.upwell_reinforced_armor
import dev.nohus.rift.generated.resources.upwell_reinforced_hull
import dev.nohus.rift.generated.resources.upwell_vulnerable_armor
import dev.nohus.rift.generated.resources.upwell_vulnerable_hull
import dev.nohus.rift.generated.resources.workforce_32px
import dev.nohus.rift.network.esi.models.SkyhookState
import dev.nohus.rift.network.esi.models.VulnerabilityWindow
import dev.nohus.rift.planetaryindustry.compose.AnnotatedProgressBar
import dev.nohus.rift.planetaryindustry.compose.ExtractionAnimation
import dev.nohus.rift.planetaryindustry.compose.NeedsAttentionAnimation
import dev.nohus.rift.planetaryindustry.compose.TitledText
import dev.nohus.rift.structures.EquinoxStructuresRepository.Skyhook
import dev.nohus.rift.structures.EquinoxStructuresRepository.SkyhookResource
import dev.nohus.rift.utils.formatDurationCompact
import dev.nohus.rift.utils.formatNumber
import dev.nohus.rift.utils.formatNumberCompact
import dev.nohus.rift.utils.plural
import dev.nohus.rift.utils.withStyle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

private val planetIconSize = 64.dp

@Composable
fun Skyhook(
    skyhook: Skyhook,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
    ) {
        Column {
            SkyhookTitleRow(skyhook)

            Row(
                modifier = Modifier
                    .padding(Spacing.medium),
            ) {
                Box(modifier = Modifier.zIndex(1f)) {
                    SkyhookIllustration(skyhook, now)
                }
                Spacer(Modifier.width(Spacing.large))
                when (skyhook.resource) {
                    is SkyhookResource.Power -> {
                        Column {
                            Spacer(Modifier.height(34.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.power_32px),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                                TitledText(
                                    title = "Produced Power",
                                    text = "${skyhook.resource.power}",
                                )
                            }
                        }
                    }
                    is SkyhookResource.Workforce -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.small),
                        ) {
                            val siphonedWorkforce = skyhook.resource.workforce - skyhook.resource.effectiveWorkforce

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                modifier = Modifier.padding(top = if (siphonedWorkforce == 0) 34.dp else 16.dp),
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.workforce_32px),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                                TitledText(
                                    title = "Produced Workforce",
                                    text = "${skyhook.resource.effectiveWorkforce}",
                                )
                            }
                            if (siphonedWorkforce > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                                ) {
                                    Box {
                                        Image(
                                            painter = painterResource(Res.drawable.mercenary_den_32px),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                        )
                                        val transition = rememberInfiniteTransition()
                                        val alpha by transition.animateFloat(
                                            initialValue = 0.7f,
                                            targetValue = 1f,
                                            animationSpec = infiniteRepeatable(tween(2000), repeatMode = RepeatMode.Reverse),
                                        )
                                        Image(
                                            painter = painterResource(Res.drawable.mercenary_den_laser_32px),
                                            contentDescription = null,
                                            alpha = alpha,
                                            modifier = Modifier
                                                .offset(x = (-29).dp)
                                                .size(32.dp),
                                        )
                                    }
                                    TitledText(
                                        title = "Mercenary Den Anarchy",
                                        text = "Siphoned workforce: ${skyhook.resource.workforce - skyhook.resource.effectiveWorkforce}",
                                    )
                                }
                            }
                        }
                    }
                    is SkyhookResource.Reagent -> {
                        Reagent(skyhook.resource, skyhook.theftVulnerability, now)
                    }
                    null -> {}
                }
            }
        }
    }
}

@Composable
private fun SkyhookIllustration(
    skyhook: Skyhook,
    now: Instant,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Row {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .zIndex(1f)
                    .padding(top = 33.dp),
            ) {
                val colorFilter = if (!skyhook.isActive) {
                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.25f) })
                } else {
                    null
                }
                Image(
                    painter = painterResource(skyhook.planet.type.icon128),
                    contentDescription = null,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .size(planetIconSize),
                )
                val transition = rememberInfiniteTransition()
                if (skyhook.isActive) {
                    ExtractionAnimation(transition)
                }
                when (skyhook.resource) {
                    is SkyhookResource.Power -> {
                        ProductIcon(Res.drawable.power_32px)
                    }
                    is SkyhookResource.Workforce -> {
                        ProductIcon(Res.drawable.workforce_32px)
                    }
                    is SkyhookResource.Reagent -> {
                        ProductIcon(skyhook.resource.icon)
                    }
                    null -> {}
                }
                if (!skyhook.isActive) {
                    NeedsAttentionAnimation(transition)
                }
            }

            Box {
                Image(
                    painter = painterResource(Res.drawable.skyhook_illustration_structure),
                    contentDescription = null,
                    modifier = Modifier
                        .zIndex(1f)
                        .size(62.5.dp, 100.dp)
                        .graphicsLayer(scaleX = -1f),
                )
                Image(
                    painter = painterResource(Res.drawable.skyhook_illustration_elevator),
                    contentDescription = null,
                    modifier = Modifier
                        .size(62.5.dp, 100.dp)
                        .graphicsLayer(scaleX = -1f),
                )
                val containerTravelDistanceRight = 37.dp
                val containerTravelDistanceLeft = (-19).dp
                val containerTravelDistanceRightPx = LocalDensity.current.run { containerTravelDistanceRight.toPx() }
                val containerTravelDistanceLeftPx = LocalDensity.current.run { containerTravelDistanceLeft.toPx() }
                val animatable = remember { Animatable(containerTravelDistanceLeftPx) }
                var isContainerFull by remember { mutableStateOf(false) }
                LaunchedEffect(skyhook.isActive) {
                    while (skyhook.isActive) {
                        isContainerFull = true
                        animatable.animateTo(
                            targetValue = containerTravelDistanceRightPx,
                            animationSpec = tween(Random.nextInt(7_000, 9_000), easing = FastOutSlowInEasing),
                        )
                        isContainerFull = false
                        animatable.animateTo(
                            targetValue = containerTravelDistanceLeftPx,
                            animationSpec = tween(Random.nextInt(2_000, 4_000), easing = FastOutSlowInEasing),
                        )
                    }
                    animatable.snapTo(containerTravelDistanceLeftPx)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(y = 60.dp)
                        .graphicsLayer {
                            translationX = animatable.value
                        },
                ) {
                    Image(
                        painter = painterResource(Res.drawable.skyhook_illustration_container),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.5.dp, 11.dp),
                    )
                    if (isContainerFull) {
                        when (skyhook.resource) {
                            is SkyhookResource.Power -> {
                                Image(
                                    painter = painterResource(Res.drawable.power_32px),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(Color.Black),
                                    modifier = Modifier.size(8.dp),
                                )
                            }
                            is SkyhookResource.Workforce -> {
                                Image(
                                    painter = painterResource(Res.drawable.workforce_32px),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(Color.Black),
                                    modifier = Modifier.size(8.dp),
                                )
                            }
                            is SkyhookResource.Reagent -> {
                                Image(
                                    painter = painterResource(skyhook.resource.icon),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(Color.Black),
                                    modifier = Modifier.size(8.dp),
                                )
                            }
                            null -> {}
                        }
                    }
                }
            }
        }

        val vulnerabilityState = when (skyhook.state) {
            SkyhookState.Unspecified -> null
            SkyhookState.ShieldVulnerable -> null
            SkyhookState.ArmorReinforced -> Res.drawable.upwell_reinforced_armor
            SkyhookState.ArmorVulnerable -> Res.drawable.upwell_vulnerable_armor
            SkyhookState.HullReinforced -> Res.drawable.upwell_reinforced_hull
            SkyhookState.HullVulnerable -> Res.drawable.upwell_vulnerable_hull
        }
        if (vulnerabilityState != null) {
            Image(
                painter = painterResource(vulnerabilityState),
                contentDescription = null,
                modifier = Modifier.height(32.dp)
            )
        }
        if (skyhook.reinforcementTimer != null) {
            val reinforcedUntil = Duration.between(now, skyhook.reinforcementTimer.end)
            Text(
                text = if (reinforcedUntil.isPositive) {
                    formatDurationCompact(reinforcedUntil)
                } else "Reinforcement ended",
                style = RiftTheme.typography.bodyPrimary,
            )
        }
    }
}

@Composable
private fun ProductIcon(
    image: DrawableResource,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(Res.drawable.pi_disc_shadow),
            contentDescription = null,
            alpha = 0.5f,
            modifier = Modifier.size(32.dp),
        )
        Image(
            painter = painterResource(image),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun SkyhookTitleRow(
    skyhook: Skyhook,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Brush.linearGradient(listOf(Color.Transparent, RiftTheme.colors.backgroundPrimary)))
            .padding(Spacing.small),
    ) {
        Column {
            val text = buildAnnotatedString {
                append("Orbital Skyhook – ")
                append(skyhook.planet.name)
                withStyle(color = RiftTheme.colors.textSecondary) {
                    when (skyhook.resource) {
                        is SkyhookResource.Power -> append(" – Power")
                        is SkyhookResource.Workforce -> append(" – Workforce")
                        is SkyhookResource.Reagent -> append(" – ${skyhook.resource.name}")
                        null -> {}
                    }
                }
            }
            Text(
                text = text,
                style = RiftTheme.typography.bodyPrimary,
            )
            Spacer(Modifier.height(Spacing.small))
            RiftSolarSystemChip(
                state = skyhook.systemChip,
            )
        }
        Spacer(Modifier.weight(1f))
        StructureOwner(skyhook.character)
    }
}

@Composable
private fun Reagent(
    resource: SkyhookResource.Reagent,
    theftVulnerability: VulnerabilityWindow?,
    now: Instant,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        StorageStock(true, resource, now)
        StorageStock(false, resource, now)
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            val unitsPerHour = resource.amountPerCycle * (resource.cyclePeriod / 3600)
            TitledText(
                title = "Units Per Hour",
                text = "${formatNumber(unitsPerHour)} units",
            )
            Vulnerability("Theft Vulnerability", theftVulnerability, now)
        }
    }
}

@Composable
private fun StorageStock(
    isSecuredBay: Boolean,
    reagent: SkyhookResource.Reagent,
    now: Instant,
) {
    val stock = if (isSecuredBay) reagent.securedStock else reagent.unsecuredStock
    val capacity = if (isSecuredBay) reagent.securedCapacity else reagent.unsecuredCapacity
    val title = if (isSecuredBay) "Secured Stock" else "Unsecured Stock"
    val stockFullTimestamp = if (isSecuredBay) reagent.securedStockFullTimestamp else reagent.unsecuredStockFullTimestamp
    RiftTooltipArea(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = RiftTheme.colors.textPrimary)) {
                append(reagent.name)
            }
            append(" ")
            appendLine()
            append("${formatNumber(stock)} unit${stock.plural}")
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            val stockVolume = stock * reagent.typeVolume
            val stockVolumeCapacity = capacity * reagent.typeVolume
            AnnotatedProgressBar(
                title = title,
                percentage = stock / capacity.toFloat(),
                description = String.format("%.0f/%.0f m3", stockVolume, stockVolumeCapacity),
                color = RiftTheme.colors.progressBarProgress,
            )

            Column {
                Text(
                    text = "Stored Resource",
                    style = RiftTheme.typography.bodyHighlighted,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val formatted = if (stock >= 10_000) formatNumberCompact(stock) else formatNumber(stock)
                    Text(
                        text = "$formatted x",
                        style = RiftTheme.typography.bodyPrimary,
                    )
                    Image(
                        painter = painterResource(reagent.icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            if (stockFullTimestamp != null) {
                val durationToFull = Duration.between(now, stockFullTimestamp)
                TitledText(
                    title = "$title Full",
                    text = "in ${formatDurationCompact(durationToFull)}",
                )
            }
        }
    }
}
