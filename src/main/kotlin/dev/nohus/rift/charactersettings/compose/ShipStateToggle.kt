package dev.nohus.rift.charactersettings.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import dev.nohus.rift.compose.RiftToggleButton
import dev.nohus.rift.compose.ToggleButtonType
import dev.nohus.rift.compose.theme.Spacing

enum class ShipState {
    InSpace, DockedStation, DockedStructure
}

@Composable
fun ShipStateToggle(
    state: ShipState,
    onChange: (ShipState) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        RiftToggleButton(
            text = "In Space",
            isSelected = state == ShipState.InSpace,
            type = ToggleButtonType.Left,
            onClick = { onChange(ShipState.InSpace) },
        )
        RiftToggleButton(
            text = "Docked in Station",
            isSelected = state == ShipState.DockedStation,
            type = ToggleButtonType.Middle,
            onClick = { onChange(ShipState.DockedStation) },
        )
        RiftToggleButton(
            text = "Docked in Structure",
            isSelected = state == ShipState.DockedStructure,
            type = ToggleButtonType.Right,
            onClick = { onChange(ShipState.DockedStructure) },
        )
    }
}
