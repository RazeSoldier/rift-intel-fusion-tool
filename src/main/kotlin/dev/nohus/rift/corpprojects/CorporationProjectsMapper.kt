package dev.nohus.rift.corpprojects

import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.corpprojects.GetProjectContributionAttributesUseCase.ProjectContributionAttributeType
import dev.nohus.rift.network.esi.models.CorporationProject
import dev.nohus.rift.network.esi.models.CorporationProjectConfiguration
import dev.nohus.rift.network.esi.models.CorporationsIdProjectsId
import dev.nohus.rift.repositories.SolarSystemChipState
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import org.koin.core.annotation.Single

@Single
class CorporationProjectsMapper {

    fun toModel(
        corporation: Corporation,
        project: CorporationProject,
        details: CorporationsIdProjectsId,
        configuration: ProjectConfiguration?,
        contributionAttributes: List<ProjectContributionAttributeType>,
        solarSystemChipState: SolarSystemChipState?,
        matchingFilters: List<ProjectCategoryFilter>,
        creator: CharacterDetailsRepository.CharacterDetails?,
        contributions: List<Contribution>,
        contributors: Contributors,
        eligibleCharacters: List<LocalCharacter>,
    ): Project {
        val details = details.let {
            ProjectDetails(
                configuration = configuration!!,
                contributionAttributes = contributionAttributes,
                solarSystemChipState = solarSystemChipState,
                matchingFilters = matchingFilters,
                participationLimit = it.contributionSettings?.participationLimit,
                rewardPerContribution = it.contributionSettings?.rewardPerContribution,
                submissionLimit = it.contributionSettings?.submissionLimit,
                submissionMultiplier = it.contributionSettings?.submissionMultiplier,
                creator = creator,
                career = it.details.career,
                created = it.details.created,
                description = it.details.description,
                expires = it.details.expires,
                finished = it.details.finished,
            )
        }
        return Project(
            corporation = corporation,
            currentProgress = project.progress.current,
            desiredProgress = project.progress.desired,
            id = project.id,
            lastModified = project.lastModified,
            name = project.name,
            reward = project.reward?.let { Reward(it.initial, it.remaining) },
            state = project.state,
            details = details,
            contributions = contributions,
            contributors = contributors,
            eligibleCharacters = eligibleCharacters,
        )
    }

    fun toModel(configuration: CorporationProjectConfiguration): ProjectConfiguration = with(configuration) {
        captureFwComplex?.let {
            return ProjectConfiguration.CaptureFwComplex(
                archetypes = it.archetypes,
                factions = it.factions,
                locations = it.locations,
            )
        }
        damageShip?.let {
            return ProjectConfiguration.DamageShip(
                identities = it.identities,
                locations = it.locations,
                ships = it.ships,
            )
        }
        defendFwComplex?.let {
            return ProjectConfiguration.DefendFwComplex(
                archetypes = it.archetypes,
                factions = it.factions,
                locations = it.locations,
            )
        }
        deliverItem?.let {
            return ProjectConfiguration.DeliverItem(
                dockingLocations = it.dockingLocations,
                items = it.items,
                officeId = it.officeId,
            )
        }
        destroyNpc?.let {
            return ProjectConfiguration.DestroyNpc(
                locations = it.locations,
            )
        }
        destroyShip?.let {
            return ProjectConfiguration.DestroyShip(
                identities = it.identities,
                locations = it.locations,
                ships = it.ships,
            )
        }
        earnLoyaltyPoint?.let {
            return ProjectConfiguration.EarnLoyaltyPoint(
                corporations = it.corporations,
            )
        }
        shipInsurance?.let {
            return ProjectConfiguration.ShipInsurance(
                conflictType = it.conflictType,
                identities = it.identities,
                locations = it.locations,
                reimburseImplants = it.reimburseImplants,
                ships = it.ships,
            )
        }
        lostShip?.let {
            return ProjectConfiguration.LostShip(
                identities = it.identities,
                locations = it.locations,
                ships = it.ships,
            )
        }
        manual?.let {
            return ProjectConfiguration.Manual
        }
        manufactureItem?.let {
            return ProjectConfiguration.ManufactureItem(
                dockingLocations = it.dockingLocations,
                items = it.items,
                owner = it.owner,
            )
        }
        mineMaterial?.let {
            return ProjectConfiguration.MineMaterial(
                locations = it.locations,
                materials = it.materials,
            )
        }
        remoteBoostShield?.let {
            return ProjectConfiguration.RemoteBoostShield(
                identities = it.identities,
                locations = it.locations,
                ships = it.ships,
            )
        }
        remoteRepairArmor?.let {
            return ProjectConfiguration.RemoteRepairArmor(
                identities = it.identities,
                locations = it.locations,
                ships = it.ships,
            )
        }
        salvageWreck?.let {
            return ProjectConfiguration.SalvageWreck(
                locations = it.locations,
            )
        }
        scanSignature?.let {
            return ProjectConfiguration.ScanSignature(
                locations = it.locations,
                signatures = it.signatures,
            )
        }
        unknown?.let {
            return ProjectConfiguration.Unknown(
                type = it.type,
            )
        }
        return ProjectConfiguration.Unknown(type = "Unknown")
    }
}
