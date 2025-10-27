package dev.nohus.rift.corpprojects

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import dev.nohus.rift.compose.AsyncAllianceLogo
import dev.nohus.rift.compose.AsyncCorporationLogo
import dev.nohus.rift.compose.AsyncPlayerPortrait
import dev.nohus.rift.compose.AsyncTypeIcon
import dev.nohus.rift.compose.ClickableAlliance
import dev.nohus.rift.compose.ClickableCharacter
import dev.nohus.rift.compose.ClickableCorporation
import dev.nohus.rift.compose.ClickableLocation
import dev.nohus.rift.compose.ClickableShip
import dev.nohus.rift.compose.ClickableSystem
import dev.nohus.rift.compose.ConstellationIllustrationIconSmall
import dev.nohus.rift.compose.RegionIllustrationIconSmall
import dev.nohus.rift.compose.RiftTooltipArea
import dev.nohus.rift.compose.SystemIllustrationIconSmall
import dev.nohus.rift.compose.VerticalGrid
import dev.nohus.rift.compose.theme.RiftTheme
import dev.nohus.rift.compose.theme.Spacing
import dev.nohus.rift.corpprojects.GetProjectContributionAttributesUseCase.ProjectContributionAttribute
import dev.nohus.rift.map.SecurityColors
import dev.nohus.rift.utils.roundSecurity
import dev.nohus.rift.utils.withColor
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun ProjectAttribute(
    caption: String,
    icon: DrawableResource?,
    values: List<ProjectContributionAttribute>,
    tooltip: String?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = caption,
            style = RiftTheme.typography.bodySecondary,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            RiftTooltipArea(
                text = tooltip,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(CutCornerShape(bottomStart = 8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .size(32.dp),
                ) {
                    if (icon != null) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = RiftTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            ProjectAttributeValuesGrid(
                values = values,
            )
        }
    }
}

@Composable
private fun ProjectAttributeValuesGrid(
    values: List<ProjectContributionAttribute>,
) {
    VerticalGrid(
        minColumnWidth = 170.dp,
        verticalSpacing = Spacing.medium,
        horizontalSpacing = Spacing.medium,
    ) {
        sort(values).forEach { value ->
            when (value) {
                is ProjectContributionAttribute.Text -> {
                    ProjectAttributeValuesGridItem(
                        type = null,
                        name = value.text,
                        alpha = if (value.isPlain) 0.1f else 0.2f,
                    )
                }

                is ProjectContributionAttribute.Type -> ProjectAttributeValuesGridItem(
                    type = "Type",
                    name = value.type.name,
                    icon = {
                        AsyncTypeIcon(
                            typeId = value.type.id,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                )

                is ProjectContributionAttribute.TypeGroup -> ProjectAttributeValuesGridItem(
                    type = "Group",
                    name = value.name,
                )

                is ProjectContributionAttribute.Ship -> ProjectAttributeValuesGridItem(
                    type = "Ship Type",
                    name = value.type.name,
                    icon = {
                        AsyncTypeIcon(
                            typeId = value.type.id,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                    decorator = {
                        ClickableShip(value.type) {
                            it()
                        }
                    },
                )

                is ProjectContributionAttribute.ShipGroup -> ProjectAttributeValuesGridItem(
                    type = "Ship Group",
                    name = value.name,
                    icon = {
                        Image(
                            painter = painterResource(value.icon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(RiftTheme.colors.textSecondary),
                            modifier = Modifier.size(24.dp),
                        )
                    },
                )

                is ProjectContributionAttribute.SolarSystem -> ProjectAttributeValuesGridItem(
                    type = "Solar System",
                    name = buildAnnotatedString {
                        append(value.solarSystem.name)
                        append(" ")
                        val security = value.solarSystem.security.roundSecurity()
                        withColor(SecurityColors[security]) {
                            append("$security")
                        }
                    },
                    icon = {
                        SystemIllustrationIconSmall(value.solarSystem.id)
                    },
                    decorator = {
                        ClickableSystem(value.solarSystem.id) {
                            it()
                        }
                    },
                )

                is ProjectContributionAttribute.Constellation -> ProjectAttributeValuesGridItem(
                    type = "Constellation",
                    name = value.constellation.name,
                    icon = {
                        ConstellationIllustrationIconSmall(value.constellation.id)
                    },
                )

                is ProjectContributionAttribute.Region -> ProjectAttributeValuesGridItem(
                    type = "Region",
                    name = value.region.name,
                    icon = {
                        RegionIllustrationIconSmall(value.region.id)
                    },
                )

                is ProjectContributionAttribute.Station -> ProjectAttributeValuesGridItem(
                    type = null,
                    name = buildAnnotatedString {
                        val security = value.solarSystem?.security?.roundSecurity()
                        if (security != null) {
                            withColor(SecurityColors[security]) {
                                append("$security")
                                append(" ")
                            }
                        }
                        append(value.station?.name ?: "Unknown Station")
                    },
                    decorator = {
                        ClickableLocation(
                            systemId = value.station?.solarSystemId,
                            locationId = value.station?.stationId?.toLong(),
                            locationTypeId = value.station?.typeId,
                            locationName = value.station?.name,
                        ) {
                            it()
                        }
                    },
                )

                is ProjectContributionAttribute.Structure -> ProjectAttributeValuesGridItem(
                    type = null,
                    name = buildAnnotatedString {
                        val security = value.solarSystem?.security?.roundSecurity()
                        if (security != null) {
                            withColor(SecurityColors[security]) {
                                append("$security")
                                append(" ")
                            }
                        }
                        append(value.structure?.name ?: "Unknown Structure")
                    },
                    decorator = {
                        ClickableLocation(
                            systemId = value.structure?.solarSystemId,
                            locationId = value.structure?.structureId,
                            locationTypeId = value.structure?.typeId,
                            locationName = value.structure?.name,
                        ) {
                            it()
                        }
                    },
                )

                is ProjectContributionAttribute.Character -> ProjectAttributeValuesGridItem(
                    type = "Capsuleer",
                    name = value.character?.name ?: "${value.id}",
                    icon = {
                        AsyncPlayerPortrait(
                            characterId = value.id,
                            size = 32,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                    decorator = {
                        ClickableCharacter(value.id) {
                            it()
                        }
                    },
                )

                is ProjectContributionAttribute.Corporation -> ProjectAttributeValuesGridItem(
                    type = "Corporation",
                    name = value.name ?: "${value.id}",
                    icon = {
                        AsyncCorporationLogo(
                            corporationId = value.id,
                            size = 32,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                    decorator = {
                        ClickableCorporation(value.id) {
                            it()
                        }
                    },
                )

                is ProjectContributionAttribute.Alliance -> ProjectAttributeValuesGridItem(
                    type = "Alliance",
                    name = value.name ?: "${value.id}",
                    icon = {
                        AsyncAllianceLogo(
                            allianceId = value.id,
                            size = 32,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                    decorator = {
                        ClickableAlliance(value.id) {
                            it()
                        }
                    },
                )

                is ProjectContributionAttribute.Faction -> ProjectAttributeValuesGridItem(
                    type = "Faction",
                    name = value.name,
                    icon = {
                        AsyncCorporationLogo(
                            corporationId = value.id,
                            size = 32,
                            modifier = Modifier.size(32.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ProjectAttributeValuesGridItem(
    type: String?,
    name: String,
    icon: @Composable (() -> Unit)? = null,
    alpha: Float = 0.2f,
    decorator: @Composable ((@Composable () -> Unit) -> Unit) = { Box(content = { it() }) },
) {
    ProjectAttributeValuesGridItem(
        type = type,
        name = AnnotatedString(name),
        icon = icon,
        alpha = alpha,
        decorator = decorator,
    )
}

@Composable
private fun ProjectAttributeValuesGridItem(
    type: String?,
    name: AnnotatedString,
    icon: @Composable (() -> Unit)? = null,
    alpha: Float = 0.2f,
    decorator: @Composable ((@Composable () -> Unit) -> Unit) = { Box(content = { it() }) },
) {
    decorator {
        Box(
            modifier = Modifier
                .clip(CutCornerShape(bottomEnd = 8.dp))
                .background(Color.White.copy(alpha = alpha))
                .fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .padding(Spacing.medium),
            ) {
                if (icon != null) {
                    icon()
                }

                Column {
                    if (type != null) {
                        Text(
                            text = type,
                            style = RiftTheme.typography.detailSecondary,
                        )
                    }
                    Text(
                        text = name,
                        style = RiftTheme.typography.bodyHighlighted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun sort(values: List<ProjectContributionAttribute>): List<ProjectContributionAttribute> {
    return values.sortedWith(
        compareBy({
            when (it) {
                is ProjectContributionAttribute.SolarSystem -> 1
                is ProjectContributionAttribute.Constellation -> 2
                is ProjectContributionAttribute.Region -> 3
                is ProjectContributionAttribute.Type -> 4
                is ProjectContributionAttribute.TypeGroup -> 5
                is ProjectContributionAttribute.Ship -> 6
                is ProjectContributionAttribute.ShipGroup -> 7
                is ProjectContributionAttribute.Character -> 8
                is ProjectContributionAttribute.Corporation -> 9
                is ProjectContributionAttribute.Alliance -> 10
                is ProjectContributionAttribute.Faction -> 11
                is ProjectContributionAttribute.Station -> 12
                is ProjectContributionAttribute.Structure -> 13
                is ProjectContributionAttribute.Text -> 14
            }
        }, {
            when (it) {
                is ProjectContributionAttribute.Alliance -> it.name
                is ProjectContributionAttribute.Character -> it.character?.name
                is ProjectContributionAttribute.Constellation -> it.constellation.name
                is ProjectContributionAttribute.Corporation -> it.name
                is ProjectContributionAttribute.Faction -> it.name
                is ProjectContributionAttribute.Region -> it.region.name
                is ProjectContributionAttribute.Ship -> it.type.name
                is ProjectContributionAttribute.ShipGroup -> it.name
                is ProjectContributionAttribute.SolarSystem -> it.solarSystem.name
                is ProjectContributionAttribute.Station -> it.station?.name
                is ProjectContributionAttribute.Structure -> it.structure?.name
                is ProjectContributionAttribute.Text -> it.text
                is ProjectContributionAttribute.Type -> it.type.name
                is ProjectContributionAttribute.TypeGroup -> it.name
            }
        }),
    )
}
