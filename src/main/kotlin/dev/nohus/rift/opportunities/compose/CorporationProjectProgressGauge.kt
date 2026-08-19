package dev.nohus.rift.opportunities.compose

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.RiftCircularCharacterPortrait
import dev.nohus.rift.compose.RiftCircularProgressGauge
import dev.nohus.rift.compose.theme.RiftTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun CorporationProjectProgressGauge(
    progress: Float,
    color: Color,
    iconResource: DrawableResource?,
    characterId: Int?,
    characterName: String?,
) {
    RiftCircularProgressGauge(
        progress = progress,
        color = color,
        diameter = 80.dp,
        gaugeWidth = 15.dp,
    ) {
        if (characterId != null) {
            RiftCircularCharacterPortrait(
                characterId = characterId,
                name = characterName ?: "",
                hasPadding = false,
                size = 48.dp,
            )
        } else if (iconResource != null) {
            Icon(
                painter = painterResource(iconResource),
                contentDescription = null,
                tint = RiftTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
