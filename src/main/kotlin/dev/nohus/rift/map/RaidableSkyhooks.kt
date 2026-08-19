package dev.nohus.rift.map

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.imageResourceRescaling
import dev.nohus.rift.compose.theme.EveColors
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.circledots_1
import dev.nohus.rift.generated.resources.circledots_2
import dev.nohus.rift.generated.resources.circledots_3
import dev.nohus.rift.generated.resources.healring2
import dev.nohus.rift.generated.resources.indicator_reagents_skyhook
import dev.nohus.rift.generated.resources.ring2
import dev.nohus.rift.repositories.PlanetTypes
import dev.nohus.rift.structures.RaidableSkyhooksRepository.RaidableSkyhook
import dev.nohus.rift.utils.withColor
import dev.nohus.rift.utils.withStyle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import java.time.Duration
import java.time.Instant

private val imageCache = mutableMapOf<Pair<DrawableResource, Int>, ImageBitmap>()
private val saveLayerPaint = Paint()

@Composable
fun RaidableSkyhookIndicator(skyhook: RaidableSkyhook, now: Instant, isShowingDetail: Boolean) {
    val isRaidableNow = skyhook.vulnerableFrom < now
    RiftTooltipArea(
        tooltip = if (!isShowingDetail) {
            @Composable {
                RaidableSkyhookDetail(skyhook, now, isRaidableNow, Modifier.padding(Spacing.medium))
            }
        } else {
            null
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Box(
                contentAlignment = Alignment.Center,
            ) {
                RaidableSkyhookGauge(skyhook, now, isRaidableNow)
                Image(
                    painter = painterResource(skyhook.planet.type.icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Image(
                    painter = painterResource(Res.drawable.indicator_reagents_skyhook),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            if (isShowingDetail) {
                RaidableSkyhookDetail(skyhook, now, isRaidableNow)
            }
        }
    }
}

@Composable
private fun RaidableSkyhookDetail(
    skyhook: RaidableSkyhook,
    now: Instant,
    isRaidable: Boolean,
    modifier: Modifier = Modifier,
) {
    val timeLeft = if (isRaidable) Duration.between(now, skyhook.vulnerableTo) else Duration.between(now, skyhook.vulnerableFrom)
    val timeLeftFormatted = if (timeLeft.toHours() > 0) {
        String.format("%d:%02d:%02d", timeLeft.toHours(), timeLeft.toMinutesPart(), timeLeft.toSecondsPart())
    } else {
        String.format("%02d:%02d", timeLeft.toMinutes(), timeLeft.toSecondsPart())
    }
    val text = buildAnnotatedString {
        if (isRaidable) {
            append("Raidable for ")
        } else {
            append("Raidable in ")
        }
        withColor(RiftTheme.colors.textHighlighted) {
            appendLine(timeLeftFormatted)
        }
        append("${skyhook.planet.name} – ")
        val color = if (skyhook.planet.type.typeId == PlanetTypes.ICE) EveColors.cryoBlue else EveColors.dangerRed
        withStyle(color = color, fontWeight = FontWeight.Bold) {
            append(skyhook.planet.type.name)
        }
    }
    Text(
        text = text,
        style = RiftTheme.typography.bodyPrimary,
        modifier = modifier,
    )
}

@Composable
private fun RaidableSkyhookGauge(
    skyhook: RaidableSkyhook,
    now: Instant,
    isRaidable: Boolean,
) {
    val dpSize = 48.dp
    val pxSize = LocalDensity.current.run { dpSize.toPx() }
    val raidableCountdownGauge = imageResourceRescaling(Res.drawable.healring2, pxSize.toInt(), imageCache)
    val unraidableCountdownGauge = imageResourceRescaling(Res.drawable.ring2, pxSize.toInt(), imageCache)
    val circleDots1 = imageResourceRescaling(Res.drawable.circledots_1, pxSize.toInt(), imageCache)
    val circleDots2 = imageResourceRescaling(Res.drawable.circledots_2, pxSize.toInt(), imageCache)
    val circleDots3 = imageResourceRescaling(Res.drawable.circledots_3, pxSize.toInt(), imageCache)
    val gaugeTint = ColorFilter.tint(EveColors.leafyGreen, BlendMode.Modulate)
    val glowTintColor = if (skyhook.planet.type.typeId == PlanetTypes.ICE) EveColors.cryoBlue else EveColors.dangerRed
    val glowTint = ColorFilter.tint(glowTintColor, BlendMode.Modulate)

    val targetTime = if (isRaidable) skyhook.vulnerableTo else skyhook.vulnerableFrom
    val percentage = Duration.between(now, targetTime).toSeconds() / Duration.ofHours(2).toSeconds().toFloat()
    val transition = rememberInfiniteTransition()

    @Composable
    fun getRotationTransition(duration: Int): State<Float> {
        return transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(animation = tween(duration, easing = LinearEasing)),
        )
    }
    val rotationAngle by getRotationTransition(30_000)
    val rotationAngleCircleDots1 by getRotationTransition(20_000)
    val rotationAngleCircleDots2 by getRotationTransition(40_000)
    val rotationAngleCircleDots3 by getRotationTransition(60_000)

    val alphaDuration = 2_000

    @Composable
    fun getAlphaTransition(offset: Int): State<Float> {
        return transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(alphaDuration, easing = LinearEasing),
                initialStartOffset = StartOffset(offsetMillis = offset, offsetType = StartOffsetType.FastForward),
                repeatMode = RepeatMode.Reverse,
            ),
        )
    }
    val alphaCircleDots1 by getAlphaTransition(0)
    val alphaCircleDots2 by getAlphaTransition(alphaDuration / 3)
    val alphaCircleDots3 by getAlphaTransition(alphaDuration * 2 / 3)

    Canvas(
        modifier = Modifier.requiredSize(dpSize),
    ) {
        drawContext.canvas.withSaveLayer(this.size.toRect(), paint = saveLayerPaint) {
            if (isRaidable) {
                rotate(-rotationAngle) {
                    val offset = 2
                    drawImage(
                        image = unraidableCountdownGauge,
                        colorFilter = glowTint,
                        dstOffset = IntOffset(offset, offset),
                        dstSize = IntSize(pxSize.toInt() - 2 * offset, pxSize.toInt() - 2 * offset),
                    )
                }
            } else {
                drawImage(
                    image = raidableCountdownGauge,
                    colorFilter = gaugeTint,
                )
            }

            drawArc(
                color = Color.Transparent,
                startAngle = 0f,
                sweepAngle = (1 - percentage) * 360f,
                useCenter = true,
                topLeft = Offset(-1f, -1f),
                size = Size(pxSize + 2f, pxSize + 2f),
                blendMode = BlendMode.Clear,
            )

            if (isRaidable) {
                rotate(rotationAngleCircleDots1) {
                    drawImage(
                        image = circleDots1,
                        colorFilter = glowTint,
                        alpha = alphaCircleDots1,
                    )
                }
                rotate(rotationAngleCircleDots2) {
                    drawImage(
                        image = circleDots2,
                        colorFilter = glowTint,
                        alpha = alphaCircleDots2,
                    )
                }
                rotate(rotationAngleCircleDots3) {
                    drawImage(
                        image = circleDots3,
                        colorFilter = glowTint,
                        alpha = alphaCircleDots3,
                    )
                }
            }
        }
    }
}
