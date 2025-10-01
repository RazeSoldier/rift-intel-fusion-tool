package dev.nohus.rift.corpprojects

import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.corpprojects.GetProjectContributionAttributesUseCase.ProjectContributionAttributeType
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.models.Archetype
import dev.nohus.rift.network.esi.models.ConflictType
import dev.nohus.rift.network.esi.models.CorporationId
import dev.nohus.rift.network.esi.models.CorporationProjectCareer
import dev.nohus.rift.network.esi.models.CorporationProjectState
import dev.nohus.rift.network.esi.models.DockableLocation
import dev.nohus.rift.network.esi.models.Faction
import dev.nohus.rift.network.esi.models.Identity
import dev.nohus.rift.network.esi.models.Item
import dev.nohus.rift.network.esi.models.Location
import dev.nohus.rift.network.esi.models.OwnerType
import dev.nohus.rift.network.esi.models.SignatureTypeId
import dev.nohus.rift.repositories.SolarSystemChipState
import dev.nohus.rift.repositories.character.CharacterDetailsRepository.CharacterDetails
import java.time.Instant

data class Project(
    val corporation: Corporation,
    val currentProgress: Long,
    val desiredProgress: Long,
    val id: String,
    val lastModified: Instant,
    val name: String,
    val reward: Reward?,
    val state: CorporationProjectState,
    val details: ProjectDetails,
    val contributions: List<Contribution>,
    val contributors: Contributors,
    val eligibleCharacters: List<LocalCharacter>,
)

data class Reward(
    val initial: Double,
    val remaining: Double,
)

data class Corporation(
    val id: Int,
    val name: String,
)

data class Contribution(
    val characterId: Int,
    val characterName: String,
    val contribution: Result<Long>,
)

sealed interface Contributors {
    data class Available(val contributors: List<Contributor>) : Contributors
    data object Empty : Contributors
    data object NoAccess : Contributors
    data class Error(val message: String) : Contributors
}

data class Contributor(
    val characterId: Int,
    val details: CharacterDetails?,
    val contributed: Long,
)

data class ProjectDetails(
    val configuration: ProjectConfiguration,
    val contributionAttributes: List<ProjectContributionAttributeType>,
    val solarSystemChipState: SolarSystemChipState?,
    val matchingFilters: List<ProjectCategoryFilter>,
    val participationLimit: Long?,
    val rewardPerContribution: Double?,
    val submissionLimit: Long?,
    val submissionMultiplier: Double?,
    val creator: CharacterDetails?,
    val career: CorporationProjectCareer,
    val created: Instant,
    val description: String,
    val expires: Instant?,
    val finished: Instant?,
)

sealed interface ProjectConfiguration {
    data class CaptureFwComplex(
        val archetypes: List<Archetype>?,
        val factions: List<Faction>?,
        val locations: List<Location>?,
    ) : ProjectConfiguration

    data class DamageShip(
        val identities: List<Identity>?,
        val locations: List<Location>?,
        val ships: List<Item>?,
    ) : ProjectConfiguration

    data class DefendFwComplex(
        val archetypes: List<Archetype>?,
        val factions: List<Faction>?,
        val locations: List<Location>?,
    ) : ProjectConfiguration

    data class DeliverItem(
        val dockingLocations: List<DockableLocation>?,
        val items: List<Item>?,
        val officeId: Long?,
    ) : ProjectConfiguration

    data class DestroyNpc(
        val locations: List<Location>?,
    ) : ProjectConfiguration

    data class DestroyShip(
        val identities: List<Identity>?,
        val locations: List<Location>?,
        val ships: List<Item>?,
    ) : ProjectConfiguration

    data class EarnLoyaltyPoint(
        val corporations: List<CorporationId>?,
    ) : ProjectConfiguration

    data class ShipInsurance(
        val conflictType: ConflictType,
        val identities: List<Identity>?,
        val locations: List<Location>?,
        val reimburseImplants: Boolean,
        val ships: List<Item>?,
    ) : ProjectConfiguration

    data class LostShip(
        val identities: List<Identity>?,
        val locations: List<Location>?,
        val ships: List<Item>?,
    ) : ProjectConfiguration

    data object Manual : ProjectConfiguration

    data class ManufactureItem(
        val dockingLocations: List<DockableLocation>?,
        val items: List<Item>?,
        val owner: OwnerType,
    ) : ProjectConfiguration

    data class MineMaterial(
        val locations: List<Location>?,
        val materials: List<Item>?,
    ) : ProjectConfiguration

    data class RemoteBoostShield(
        val identities: List<Identity>?,
        val locations: List<Location>?,
        val ships: List<Item>?,
    ) : ProjectConfiguration

    data class RemoteRepairArmor(
        val identities: List<Identity>?,
        val locations: List<Location>?,
        val ships: List<Item>?,
    ) : ProjectConfiguration

    data class SalvageWreck(
        val locations: List<Location>?,
    ) : ProjectConfiguration

    data class ScanSignature(
        val locations: List<Location>?,
        val signatures: List<SignatureTypeId>?,
    ) : ProjectConfiguration

    data class Unknown(
        val type: String,
    ) : ProjectConfiguration
}
