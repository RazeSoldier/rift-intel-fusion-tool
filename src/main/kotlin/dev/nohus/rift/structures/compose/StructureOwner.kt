package dev.nohus.rift.structures.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncAllianceLogo
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.CharacterTooltip
import dev.nohus.rift.compose.ClickableAlliance
import dev.nohus.rift.compose.ClickableCharacter
import dev.nohus.rift.compose.ClickableCorporation
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.dynamicportraits.DynamicCharacterPortraitParallax
import dev.nohus.rift.repositories.character.CharacterDetailsRepository.CharacterDetails

@Composable
fun StructureOwner(
    character: CharacterDetails,
    isShowingCharacter: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        if (isShowingCharacter) {
            ClickableCharacter(character.characterId) {
                CharacterTooltip(character) {
                    DynamicCharacterPortraitParallax(
                        characterId = character.characterId,
                        size = 32.dp,
                        enterTimestamp = null,
                        pointerInteractionStateHolder = null,
                    )
                }
            }
        }
        ClickableCorporation(character.corporationId) {
            AsyncCorporationLogo(
                corporationId = character.corporationId,
                size = 64,
                modifier = Modifier
                    .size(32.dp),
            )
        }
        if (character.allianceId != null) {
            ClickableAlliance(character.allianceId) {
                AsyncAllianceLogo(
                    allianceId = character.allianceId,
                    size = 64,
                    modifier = Modifier
                        .size(32.dp),
                )
            }
        }
        Column {
            if (isShowingCharacter) {
                ClickableCharacter(character.characterId) {
                    Text(
                        text = character.name,
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
                ClickableCorporation(character.corporationId) {
                    Text(
                        text = character.corporationName,
                        style = RiftTheme.typography.detailPrimary,
                    )
                }
            } else {
                ClickableCorporation(character.corporationId) {
                    Text(
                        text = character.corporationName,
                        style = RiftTheme.typography.bodyPrimary,
                    )
                }
                ClickableAlliance(character.allianceId) {
                    Text(
                        text = character.allianceName ?: "",
                        style = RiftTheme.typography.detailPrimary,
                    )
                }
            }
        }
    }
}
