package dev.nohus.rift.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.repositories.character.CharacterDetailsRepository

@Composable
fun RiftCircularCharacterPortrait(
    characterId: Int?,
    characterDetails: CharacterDetailsRepository.CharacterDetails?,
    name: String,
    hasPadding: Boolean,
    size: Dp,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(RiftTheme.colors.windowBackgroundActive.copy(alpha = 0.3f))
            .modifyIf(hasPadding) {
                padding(4.dp)
            },
    ) {
        CharacterTooltip(characterDetails) {
            ClickableCharacter(characterId) {
                if (characterId != null) {
                    DynamicCharacterPortraitParallax(
                        characterId = characterId,
                        size = size,
                        enterTimestamp = null,
                        pointerInteractionStateHolder = null,
                        modifier = Modifier
                            .clip(CircleShape),
                    )
                } else {
                    AsyncCharacterPortrait(
                        characterId = characterId,
                        size = 64,
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape),
                    )
                }
            }
        }
    }
}
