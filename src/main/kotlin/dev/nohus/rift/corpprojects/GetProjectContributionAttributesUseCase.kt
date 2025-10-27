package dev.nohus.rift.corpprojects

import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.contribution_method_earn_loyalty_point_16px
import dev.nohus.rift.generated.resources.contribution_method_manual_16px
import dev.nohus.rift.generated.resources.contribution_method_scan_signatures_16px
import dev.nohus.rift.generated.resources.corporation_16px
import dev.nohus.rift.generated.resources.fw_complex_type_16px
import dev.nohus.rift.generated.resources.location_16px
import dev.nohus.rift.generated.resources.map_marker_resources_storage
import dev.nohus.rift.generated.resources.mining_16px
import dev.nohus.rift.generated.resources.pilot_or_organization_16px
import dev.nohus.rift.generated.resources.spaceship_command_16px
import dev.nohus.rift.location.LocationRepository
import dev.nohus.rift.network.esi.models.Archetype
import dev.nohus.rift.network.esi.models.ConflictType
import dev.nohus.rift.network.esi.models.CorporationId
import dev.nohus.rift.network.esi.models.DockableLocation
import dev.nohus.rift.network.esi.models.Faction
import dev.nohus.rift.network.esi.models.Identity
import dev.nohus.rift.network.esi.models.Item
import dev.nohus.rift.network.esi.models.Location
import dev.nohus.rift.network.esi.models.OwnerType
import dev.nohus.rift.network.esi.models.SignatureTypeId
import dev.nohus.rift.repositories.FactionNames
import dev.nohus.rift.repositories.ShipTreeGroups
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapConstellation
import dev.nohus.rift.repositories.SolarSystemsRepository.MapRegion
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository.CharacterDetails
import dev.nohus.rift.utils.mapAsync
import org.jetbrains.compose.resources.DrawableResource
import org.koin.core.annotation.Single

@Single
class GetProjectContributionAttributesUseCase(
    private val solarSystemsRepository: SolarSystemsRepository,
    private val typesRepository: TypesRepository,
    private val characterDetailsRepository: CharacterDetailsRepository,
    private val locationRepository: LocationRepository,
) {

    data class ProjectContributionAttributeType(
        val name: String,
        val description: String,
        val icon: DrawableResource,
        val values: List<ProjectContributionAttribute>,
    )

    sealed interface ProjectContributionAttribute {
        data class Text(val text: String, val isPlain: Boolean = false) : ProjectContributionAttribute
        data class Type(val type: TypesRepository.Type) : ProjectContributionAttribute
        data class TypeGroup(val name: String) : ProjectContributionAttribute
        data class Ship(val type: TypesRepository.Type) : ProjectContributionAttribute
        data class ShipGroup(val name: String, val icon: DrawableResource) : ProjectContributionAttribute
        data class SolarSystem(val solarSystem: MapSolarSystem) : ProjectContributionAttribute
        data class Constellation(val constellation: MapConstellation) : ProjectContributionAttribute
        data class Region(val region: MapRegion) : ProjectContributionAttribute
        data class Station(val station: LocationRepository.Station?, val solarSystem: MapSolarSystem?) : ProjectContributionAttribute
        data class Structure(val structure: LocationRepository.Structure?, val solarSystem: MapSolarSystem?) : ProjectContributionAttribute
        data class Faction(val id: Int, val name: String) : ProjectContributionAttribute
        data class Character(val id: Int, val character: CharacterDetails?) : ProjectContributionAttribute
        data class Corporation(val id: Int, val name: String?) : ProjectContributionAttribute
        data class Alliance(val id: Int, val name: String?) : ProjectContributionAttribute
    }

    suspend operator fun invoke(configuration: ProjectConfiguration, characterId: Int): List<ProjectContributionAttributeType> {
        return when (val configuration = configuration) {
            is ProjectConfiguration.CaptureFwComplex -> listOf(
                ProjectContributionAttributeType(
                    name = "Complex Type",
                    description = "The type of complex that\nmust be captured.",
                    icon = Res.drawable.fw_complex_type_16px,
                    values = configuration.archetypes?.map(::mapArchetype) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Solar System",
                    description = "The solar system in which\ncomplexes must be captured.",
                    icon = Res.drawable.location_16px,
                    values = configuration.locations?.mapNotNull(::mapLocation) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Faction to capture for",
                    description = "The faction you must capture\ncomplexes for.",
                    icon = Res.drawable.corporation_16px,
                    values = configuration.factions?.map(::mapFaction) ?: any(),
                ),
            )
            is ProjectConfiguration.DamageShip -> listOf(
                ProjectContributionAttributeType(
                    name = "Capsuleers or Organisations",
                    description = "Either the capsuleers who must\nbe piloting the ships being\ndamaged, or the corporations,\nalliances, or factions to which\nthe damaged ship's pilot must\nbelong.",
                    icon = Res.drawable.pilot_or_organization_16px,
                    values = configuration.identities?.mapAsync { mapIdentity(it) }?.filterNotNull() ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Ship Types or Groups",
                    description = "The types or groups of ships\nto which damage must be\ndealt.",
                    icon = Res.drawable.spaceship_command_16px,
                    values = configuration.ships?.mapNotNull(::mapShip) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Locations",
                    description = "Solar Systems, Constellations,\nor Regions in which the\ndamage to capsuleer ships\nmust be dealt.",
                    icon = Res.drawable.location_16px,
                    values = configuration.locations?.mapNotNull(::mapLocation) ?: any(),
                ),
            )
            is ProjectConfiguration.DefendFwComplex -> listOf(
                ProjectContributionAttributeType(
                    name = "Complex Type",
                    description = "The type of complex that\nmust be defended.",
                    icon = Res.drawable.fw_complex_type_16px,
                    values = configuration.archetypes?.map(::mapArchetype) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Solar System",
                    description = "The solar system in which\ncomplexes must be defended.",
                    icon = Res.drawable.location_16px,
                    values = configuration.locations?.mapNotNull(::mapLocation) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Faction to capture for",
                    description = "The faction whose complexes\nyou must defend.",
                    icon = Res.drawable.corporation_16px,
                    values = configuration.factions?.map(::mapFaction) ?: any(),
                ),
            )
            is ProjectConfiguration.DeliverItem -> listOf(
                ProjectContributionAttributeType(
                    name = "Item Type",
                    description = "The type of item to be delivered.",
                    icon = Res.drawable.map_marker_resources_storage,
                    values = configuration.items?.mapNotNull(::mapItem) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Destination",
                    description = "The Corporation Office to which\nitems must be delivered.",
                    icon = Res.drawable.map_marker_resources_storage,
                    values = configuration.officeId?.let { listOf(mapOfficeId(it)) } ?: any("Any Corp Office"),
                ),
            )
            is ProjectConfiguration.DestroyNpc -> listOf(
                ProjectContributionAttributeType(
                    name = "Solar System",
                    description = "The solar system in which the\nship must be destroyed.",
                    icon = Res.drawable.location_16px,
                    values = configuration.locations?.mapNotNull(::mapLocation) ?: any(),
                ),
            )
            is ProjectConfiguration.DestroyShip -> listOf(
                ProjectContributionAttributeType(
                    name = "Capsuleers or Organisations",
                    description = "Either the capsuleers who must\nown the destroyed ships, or\nthe organizations they must\nbelong to (corporation, alliance,\nfaction).",
                    icon = Res.drawable.pilot_or_organization_16px,
                    values = configuration.identities?.mapAsync { mapIdentity(it) }?.filterNotNull() ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Ship Types or Groups",
                    description = "The types or groups of ships\nthat must be destroyed.",
                    icon = Res.drawable.spaceship_command_16px,
                    values = configuration.ships?.mapNotNull(::mapShip) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Locations",
                    description = "Solar Systems, Constellations,\nor Regions in which the ship\nmust be destroyed.",
                    icon = Res.drawable.location_16px,
                    values = configuration.locations?.mapNotNull(::mapLocation) ?: any(),
                ),
            )
            is ProjectConfiguration.EarnLoyaltyPoint -> listOf(
                ProjectContributionAttributeType(
                    name = "Corporations",
                    description = "The corporations that have\nissued the Loyalty Points.",
                    icon = Res.drawable.contribution_method_earn_loyalty_point_16px,
                    values = configuration.corporations?.mapAsync { mapCorporationId(it) } ?: any(),
                ),
            )
            is ProjectConfiguration.LostShip -> listOf(
                ProjectContributionAttributeType(
                    name = "Capsuleers or Organisations",
                    description = "The capsuleer or organization\nresponsible for the destruction\nof the ship.",
                    icon = Res.drawable.pilot_or_organization_16px,
                    values = configuration.identities?.mapAsync { mapIdentity(it) }?.filterNotNull() ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Ship Type",
                    description = "The type of the ship lost.",
                    icon = Res.drawable.spaceship_command_16px,
                    values = configuration.ships?.mapNotNull(::mapShip) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Solar System",
                    description = "The solar system in which the\nship was lost.",
                    icon = Res.drawable.location_16px,
                    values = configuration.locations?.mapNotNull(::mapLocation) ?: any(),
                ),
            )
            ProjectConfiguration.Manual -> listOf()
            is ProjectConfiguration.ManufactureItem -> listOf(
                ProjectContributionAttributeType(
                    name = "On the Behalf Of",
                    description = "On whose behalf the job must\nbe run.",
                    icon = Res.drawable.contribution_method_manual_16px,
                    values = listOf(mapOwnerType(configuration.owner)),
                ),
                ProjectContributionAttributeType(
                    name = "Item Type",
                    description = "The type of item to be\nmanufactured.",
                    icon = Res.drawable.map_marker_resources_storage,
                    values = configuration.items?.mapNotNull(::mapItem) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Station or Structure",
                    description = "The station or structure in\nwhich the manufacturing jobs\nmust be installed.",
                    icon = Res.drawable.location_16px,
                    values = configuration.dockingLocations?.mapAsync {
                        mapDockableLocation(it, characterId)
                    }?.filterNotNull() ?: any(),
                ),
            )
            is ProjectConfiguration.MineMaterial -> listOf(
                ProjectContributionAttributeType(
                    name = "Material Types or Groups",
                    description = "The types or groups of ore or\nice products to be mined.",
                    icon = Res.drawable.mining_16px,
                    values = configuration.materials?.mapNotNull(::mapItem) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Locations",
                    description = "Solar Systems, Constellations,\nor Regions in which the\nmaterials must be mined.",
                    icon = Res.drawable.location_16px,
                    values = configuration.locations?.mapNotNull(::mapLocation) ?: any(),
                ),
            )
            is ProjectConfiguration.RemoteBoostShield -> listOf(
                ProjectContributionAttributeType(
                    name = "Capsuleer or Organisation",
                    description = "Either the capsuleer who must\nown the ship being remote\nboosted, or an organization\nthey must belong to\n(corporation, alliance, faction).",
                    icon = Res.drawable.pilot_or_organization_16px,
                    values = configuration.identities?.mapAsync { mapIdentity(it) }?.filterNotNull() ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Ship Type",
                    description = "The type of ship whose shield\nmust be boosted.",
                    icon = Res.drawable.spaceship_command_16px,
                    values = configuration.ships?.mapNotNull(::mapShip) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Solar System",
                    description = "The solar system in which the\nshield must be boosted.",
                    icon = Res.drawable.location_16px,
                    values = configuration.locations?.mapNotNull(::mapLocation) ?: any(),
                ),
            )
            is ProjectConfiguration.RemoteRepairArmor -> listOf(
                ProjectContributionAttributeType(
                    name = "Capsuleer or Organisation",
                    description = "Either the capsuleer who must\nown the ship being remote\nrepaired, or an organization\nthey must belong to\n(corporation, alliance, faction).",
                    icon = Res.drawable.pilot_or_organization_16px,
                    values = configuration.identities?.mapAsync { mapIdentity(it) }?.filterNotNull() ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Ship Type",
                    description = "The type of ship whose armor\nmust be repaired.",
                    icon = Res.drawable.spaceship_command_16px,
                    values = configuration.ships?.mapNotNull(::mapShip) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Solar System",
                    description = "The solar system in which the\narmor must be repaired.",
                    icon = Res.drawable.location_16px,
                    values = configuration.locations?.mapNotNull(::mapLocation) ?: any(),
                ),
            )
            is ProjectConfiguration.SalvageWreck -> listOf(
                ProjectContributionAttributeType(
                    name = "Solar System",
                    description = "The solar system in which the\nsalvaging must take place.",
                    icon = Res.drawable.location_16px,
                    values = configuration.locations?.mapNotNull(::mapLocation) ?: any(),
                ),
            )
            is ProjectConfiguration.ScanSignature -> listOf(
                ProjectContributionAttributeType(
                    name = "Signature Type",
                    description = "The type of cosmic signature\nwhich must be scanned.",
                    icon = Res.drawable.contribution_method_scan_signatures_16px,
                    values = configuration.signatures?.map(::mapSignatureTypeId) ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Solar System",
                    description = "The solar system in which the\nscanned signature must be\nlocated.",
                    icon = Res.drawable.location_16px,
                    values = configuration.locations?.mapNotNull(::mapLocation) ?: any(),
                ),
            )
            is ProjectConfiguration.ShipInsurance -> listOf(
                ProjectContributionAttributeType(
                    name = "Require Capsuleer Involvement?",
                    description = "Require that another capsuleer\nbe involved in the destruction\nof the ship.",
                    icon = Res.drawable.contribution_method_manual_16px,
                    values = listOf(mapConflictType(configuration.conflictType)),
                ),
                ProjectContributionAttributeType(
                    name = "Capsuleers or Organisations",
                    description = "Capsuleers or organizations\nresponsible for the destruction\nof the ship.",
                    icon = Res.drawable.pilot_or_organization_16px,
                    values = configuration.identities?.mapAsync { mapIdentity(it) }?.filterNotNull() ?: any(),
                ),
                ProjectContributionAttributeType(
                    name = "Ship Types or Groups",
                    description = "Types or Groups of ships that\nare covered by this project. If\nempty, all ship types are covered.",
                    icon = Res.drawable.spaceship_command_16px,
                    values = configuration.ships?.mapNotNull(::mapShip) ?: any(
                        if (configuration.reimburseImplants) {
                            "Any – implants losses covered on capsule loss"
                        } else {
                            "Any – implants losses not covered on capsule loss"
                        },
                    ),
                ),
                ProjectContributionAttributeType(
                    name = "Locations",
                    description = "Solar Systems, Constellations,\nor Regions in which the loss\nmust take place.",
                    icon = Res.drawable.location_16px,
                    values = configuration.locations?.mapNotNull(::mapLocation) ?: any(),
                ),
            )
            is ProjectConfiguration.Unknown -> listOf()
        }
    }

    private fun mapLocation(location: Location): ProjectContributionAttribute? {
        return when {
            location.solarSystemId != null -> solarSystemsRepository.getSystem(location.solarSystemId.toInt())
                ?.let { ProjectContributionAttribute.SolarSystem(it) }
            location.constellationId != null -> solarSystemsRepository.getConstellation(location.constellationId.toInt())
                ?.let { ProjectContributionAttribute.Constellation(it) }
            location.regionId != null -> solarSystemsRepository.getRegion(location.regionId.toInt())
                ?.let { ProjectContributionAttribute.Region(it) }
            else -> null
        }
    }

    private fun mapItem(item: Item): ProjectContributionAttribute? {
        return when {
            item.groupId != null -> {
                typesRepository.getGroupName(item.groupId.toInt())?.let {
                    ProjectContributionAttribute.TypeGroup(it)
                }
            }
            item.typeId != null -> {
                typesRepository.getType(item.typeId.toInt())?.let {
                    ProjectContributionAttribute.Type(it)
                }
            }
            else -> null
        }
    }

    private fun mapShip(item: Item): ProjectContributionAttribute? {
        return when {
            item.groupId != null -> {
                ShipTreeGroups[item.groupId.toInt()]?.let {
                    ProjectContributionAttribute.ShipGroup(it.name, it.icon)
                }
            }
            item.typeId != null -> {
                typesRepository.getType(item.typeId.toInt())?.let {
                    ProjectContributionAttribute.Ship(it)
                }
            }
            else -> null
        }
    }

    private fun mapFaction(faction: Faction): ProjectContributionAttribute {
        val name = FactionNames[faction.factionId.toInt()]
        return ProjectContributionAttribute.Faction(faction.factionId.toInt(), name)
    }

    private fun mapArchetype(archetype: Archetype): ProjectContributionAttribute {
        val name = when (archetype.archetypeId.toInt()) {
            33 -> "Novice Complex"
            34 -> "Small Complex"
            35 -> "Medium Complex"
            36 -> "Large Complex"
            else -> "Unknown Complex"
        }
        return ProjectContributionAttribute.Text(name)
    }

    private suspend fun mapIdentity(identity: Identity): ProjectContributionAttribute? {
        return when {
            identity.characterId != null -> {
                val details = characterDetailsRepository.getCharacterDetails(identity.characterId.toInt())
                return ProjectContributionAttribute.Character(identity.characterId.toInt(), details)
            }
            identity.corporationId != null -> {
                val name = characterDetailsRepository.getCorporationName(identity.corporationId.toInt()).success?.name
                return ProjectContributionAttribute.Corporation(identity.corporationId.toInt(), name)
            }
            identity.allianceId != null -> {
                val name = characterDetailsRepository.getAllianceName(identity.allianceId.toInt()).success?.name
                return ProjectContributionAttribute.Alliance(identity.allianceId.toInt(), name)
            }
            identity.factionId != null -> {
                val name = FactionNames[identity.factionId.toInt()]
                return ProjectContributionAttribute.Faction(identity.factionId.toInt(), name)
            }
            else -> null
        }
    }

    private suspend fun mapCorporationId(corporationId: CorporationId): ProjectContributionAttribute {
        val name = characterDetailsRepository.getCorporationName(corporationId.corporationId.toInt()).success?.name
        return ProjectContributionAttribute.Corporation(corporationId.corporationId.toInt(), name)
    }

    private suspend fun mapDockableLocation(dockableLocation: DockableLocation, characterId: Int): ProjectContributionAttribute? {
        return when {
            dockableLocation.stationId != null -> {
                val station = locationRepository.getStation(dockableLocation.stationId.toInt())
                val solarSystem = station?.solarSystemId?.let { solarSystemsRepository.getSystem(it) }
                ProjectContributionAttribute.Station(station, solarSystem)
            }
            dockableLocation.structureId != null -> {
                val structure = locationRepository.getStructure(dockableLocation.structureId, characterId)
                val solarSystem = structure?.solarSystemId?.let { solarSystemsRepository.getSystem(it) }
                ProjectContributionAttribute.Structure(structure, solarSystem)
            }
            else -> null
        }
    }

    private fun mapOwnerType(ownerType: OwnerType): ProjectContributionAttribute {
        val text = when (ownerType) {
            OwnerType.Any -> "Any"
            OwnerType.Corporation -> "Corporation"
            OwnerType.Character -> "Self"
        }
        return ProjectContributionAttribute.Text(text)
    }

    private fun mapSignatureTypeId(signatureTypeId: SignatureTypeId): ProjectContributionAttribute {
        val name = when (signatureTypeId.signatureTypeId.toInt()) {
            208 -> "Data Site"
            209 -> "Gas Site"
            210 -> "Relic Site"
            211 -> "Ore Site"
            1136 -> "Combat Site"
            1908 -> "Wormhole"
            else -> "Unknown Signature Type"
        }
        return ProjectContributionAttribute.Text(name)
    }

    private fun mapConflictType(conflictType: ConflictType): ProjectContributionAttribute {
        val text = when (conflictType) {
            ConflictType.Pvp -> "Required – only covers losses that involved a capsuleer attacked"
            ConflictType.Pve, ConflictType.Any -> "Unrequired – covers losses regardless of capsuleer attacker involvement"
        }
        return ProjectContributionAttribute.Text(text)
    }

    private fun mapOfficeId(officeId: Long): ProjectContributionAttribute {
        return ProjectContributionAttribute.Text("Specific Corp Office")
    }

    private fun any(text: String = "Any"): List<ProjectContributionAttribute.Text> {
        return listOf(ProjectContributionAttribute.Text(text, isPlain = true))
    }
}
