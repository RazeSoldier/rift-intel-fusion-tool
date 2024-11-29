package dev.nohus.rift.standings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.nohus.rift.compose.theme.RiftTheme

@Composable
fun Standing.getColor(): Color? {
    return when (this) {
        Standing.Terrible -> RiftTheme.colors.standingTerrible
        Standing.Bad -> RiftTheme.colors.standingBad
        Standing.Neutral -> null
        Standing.Good -> RiftTheme.colors.standingGood
        Standing.Excellent -> RiftTheme.colors.standingExcellent
    }
}

fun Standing.getSystemColor(): Color {
    return when (this) {
        Standing.Terrible -> Color(0xFFBB1116)
        Standing.Bad -> Color(0xFFCE440F)
        Standing.Neutral -> Color(0xFF8D3163)
        Standing.Good -> Color(0xFF4ECEF8)
        Standing.Excellent -> Color(0xFF2C75E1)
    }
}

val Standing.isFriendly: Boolean get() {
    return when (this) {
        Standing.Terrible -> false
        Standing.Bad -> false
        Standing.Neutral -> false
        Standing.Good -> true
        Standing.Excellent -> true
    }
}
