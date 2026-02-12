package dev.nohus.rift.opportunities

import androidx.compose.ui.text.AnnotatedString
import dev.nohus.rift.compose.RiftOpportunityCardCategory
import dev.nohus.rift.compose.RiftOpportunityCardType
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.i18n.getStringSync
import dev.nohus.rift.generated.resources.*
import dev.nohus.rift.network.esi.models.BroadcastLocation
import dev.nohus.rift.network.esi.models.OpportunityCareer
import dev.nohus.rift.opportunities.GetOpportunityContributionAttributesUseCase.OpportunityContributionAttribute
import dev.nohus.rift.opportunities.GetOpportunityContributionAttributesUseCase.OpportunityContributionAttributeType
import dev.nohus.rift.repositories.GetSolarSystemChipStateUseCase
import dev.nohus.rift.repositories.SolarSystemChipLocation
import dev.nohus.rift.repositories.SolarSystemChipState
import org.jetbrains.compose.resources.DrawableResource
import java.math.BigInteger
import java.util.UUID

object OpportunitiesUtils {

    fun getOpportunityCategory(opportunity: Opportunity): RiftOpportunityCardCategory {
        return when (opportunity.details.career) {
            OpportunityCareer.Unspecified -> RiftOpportunityCardCategory.Unclassified
            OpportunityCareer.Explorer -> RiftOpportunityCardCategory.Explorer
            OpportunityCareer.Industrialist -> RiftOpportunityCardCategory.Industrialist
            OpportunityCareer.Enforcer -> RiftOpportunityCardCategory.Enforcer
            OpportunityCareer.SoldierOfFortune -> RiftOpportunityCardCategory.SoldierOfFortune
        }
    }

    data class OpportunityCategoryMetadata(
        val name: String,
        val icon: DrawableResource? = null,
        val tooltip: String? = null,
        val progressUnit: String?,
        val rewardPer: String?,
    )

    fun getOpportunityType(opportunity: Opportunity): RiftOpportunityCardType {
        val metadata = getOpportunityTypeMetadata(opportunity)
        return RiftOpportunityCardType(
            text = AnnotatedString(metadata.name),
            icon = metadata.icon,
            tooltip = metadata.tooltip,
        )
    }

    fun getOpportunityTypeMetadata(opportunity: Opportunity): OpportunityCategoryMetadata {
        return when (val configuration = opportunity.details.configuration) {
            is OpportunityConfiguration.CaptureFwComplex -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_capture_fw_complexes),
                icon = Res.drawable.contribution_method_attack_fw_complex_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_capture_fw_complexes_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_capture_fw_complexes_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_capture_fw_complexes_rewardper),
            )

            is OpportunityConfiguration.DamageShip -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_damage_ship),
                icon = Res.drawable.contribution_method_damage_ship_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_damage_ship_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_damage_ship_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_damage_ship_rewardper),
            )

            is OpportunityConfiguration.DefendFwComplex -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_defend_fw_complex),
                icon = Res.drawable.contribution_method_defend_fw_complex_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_defend_fw_complex_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_defend_fw_complex_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_defend_fw_complex_rewardper),
            )

            is OpportunityConfiguration.DeliverItem -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_deliver),
                icon = Res.drawable.contribution_method_deliver_item_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_deliver_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_deliver_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_deliver_rewardper),
            )

            is OpportunityConfiguration.DestroyNpc -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_destroy_npc),
                icon = Res.drawable.contribution_method_destroy_npc_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_destroy_npc_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_destroy_npc_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_destroy_npc_rewardper),
            )

            is OpportunityConfiguration.DestroyShip -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_destroy_ship),
                icon = Res.drawable.contribution_method_destroy_ship_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_destroy_ship_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_destroy_ship_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_destroy_ship_rewardper),
            )

            is OpportunityConfiguration.EarnLoyaltyPoint -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_earn_lp_point),
                icon = Res.drawable.contribution_method_earn_loyalty_point_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_earn_lp_point_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_earn_lp_point_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_earn_lp_point_rewardper),
            )

            is OpportunityConfiguration.LostShip -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_lost_ship),
                icon = Res.drawable.contribution_method_lost_ship_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_lost_ship_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_lost_ship_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_lost_ship_rewardper),
            )

            OpportunityConfiguration.Manual -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_manual),
                icon = Res.drawable.contribution_method_manual_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_manual_tooltip),
                progressUnit = null,
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_manual_rewardper),
            )

            is OpportunityConfiguration.ManufactureItem -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_manufacture),
                icon = Res.drawable.contribution_method_manufacture_item_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_manufacture_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_manufacture_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_manufacture_rewardper),
            )

            is OpportunityConfiguration.MineMaterial -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_mine),
                icon = Res.drawable.contribution_method_mine_material_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_mine_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_mine_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_mine_rewardper),
            )

            is OpportunityConfiguration.RemoteBoostShield -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_remote_boost_shield),
                icon = Res.drawable.contribution_method_remote_boost_shields_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_remote_boost_shield_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_remote_boost_shield_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_remote_boost_shield_rewardper),
            )

            is OpportunityConfiguration.RemoteRepairArmor -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_remote_repair_armor),
                icon = Res.drawable.contribution_method_remote_repair_armor_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_remote_repair_armor_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_remote_repair_armor_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_remote_repair_armor_rewardper),
            )

            is OpportunityConfiguration.SalvageWreck -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_salvage_wrecks),
                icon = Res.drawable.contribution_method_salvage_wreck_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_salvage_wrecks_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_salvage_wrecks_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_salvage_wrecks_rewardper),
            )

            is OpportunityConfiguration.ScanSignature -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_scan_signatures),
                icon = Res.drawable.contribution_method_scan_signatures_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_scan_signatures_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_scan_signatures_progress_unit),
                rewardPer = getStringSync(Res.string.opportunities_window_opportunity_type_scan_signatures_rewardper),
            )

            is OpportunityConfiguration.ShipInsurance -> OpportunityCategoryMetadata(
                name = getStringSync(Res.string.opportunities_window_opportunity_type_ship_insurance),
                icon = Res.drawable.contribution_method_ship_insurance_16px,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_ship_insurance_tooltip),
                progressUnit = getStringSync(Res.string.opportunities_window_opportunity_type_ship_insurance_progress_unit),
                rewardPer = null,
            )

            is OpportunityConfiguration.Unknown -> OpportunityCategoryMetadata(
                name = configuration.type,
                tooltip = getStringSync(Res.string.opportunities_window_opportunity_type_unknown_tooltip),
                progressUnit = null,
                rewardPer = null,
            )
        }
    }

    fun getMatchingFilters(
        baseType: OpportunityCategoryFilter,
        career: OpportunityCareer,
        configuration: OpportunityConfiguration?,
    ): List<OpportunityCategoryFilter> {
        return buildList {
            add(baseType)

            career.let {
                when (it) {
                    OpportunityCareer.Explorer -> OpportunityCategoryFilter.Explorer
                    OpportunityCareer.Industrialist -> OpportunityCategoryFilter.Industrialist
                    OpportunityCareer.Enforcer -> OpportunityCategoryFilter.Enforcer
                    OpportunityCareer.SoldierOfFortune -> OpportunityCategoryFilter.SoldierOfFortune
                    else -> null
                }
            }?.let { add(it) }

            when (configuration) {
                is OpportunityConfiguration.CaptureFwComplex -> listOf(OpportunityCategoryFilter.FactionalWarfare, OpportunityCategoryFilter.Combat)
                is OpportunityConfiguration.DamageShip -> listOf(OpportunityCategoryFilter.Combat)
                is OpportunityConfiguration.DefendFwComplex -> listOf(OpportunityCategoryFilter.FactionalWarfare, OpportunityCategoryFilter.Combat)
                is OpportunityConfiguration.DeliverItem -> listOf(OpportunityCategoryFilter.Hauling)
                is OpportunityConfiguration.DestroyNpc -> listOf(OpportunityCategoryFilter.Combat)
                is OpportunityConfiguration.DestroyShip -> listOf(OpportunityCategoryFilter.Combat)
                is OpportunityConfiguration.EarnLoyaltyPoint -> listOf()
                is OpportunityConfiguration.LostShip -> listOf(OpportunityCategoryFilter.Combat, OpportunityCategoryFilter.Fleet, OpportunityCategoryFilter.Logistics)
                OpportunityConfiguration.Manual -> listOf()
                is OpportunityConfiguration.ManufactureItem -> listOf(OpportunityCategoryFilter.Manufacturing)
                is OpportunityConfiguration.MineMaterial -> listOf(OpportunityCategoryFilter.Mining)
                is OpportunityConfiguration.RemoteBoostShield -> listOf(OpportunityCategoryFilter.Combat, OpportunityCategoryFilter.Fleet, OpportunityCategoryFilter.Logistics)
                is OpportunityConfiguration.RemoteRepairArmor -> listOf(OpportunityCategoryFilter.Combat, OpportunityCategoryFilter.Fleet, OpportunityCategoryFilter.Logistics)
                is OpportunityConfiguration.SalvageWreck -> listOf()
                is OpportunityConfiguration.ScanSignature -> listOf(OpportunityCategoryFilter.CosmicSignatures)
                is OpportunityConfiguration.ShipInsurance -> listOf(OpportunityCategoryFilter.Combat, OpportunityCategoryFilter.Fleet, OpportunityCategoryFilter.Logistics)
                is OpportunityConfiguration.Unknown -> listOf()
                null -> listOf()
            }.let { addAll(it) }
        }
    }

    fun getSolarSystemChipState(
        getSolarSystemChipStateUseCase: GetSolarSystemChipStateUseCase,
        contributionAttributes: List<OpportunityContributionAttributeType>,
        broadcastLocations: List<BroadcastLocation>? = null,
    ): SolarSystemChipState? {
        val attributeLocations = contributionAttributes.flatMap { it.values }.mapNotNull { value ->
            when (value) {
                is OpportunityContributionAttribute.SolarSystem -> SolarSystemChipLocation.SolarSystem(value.solarSystem.id)
                is OpportunityContributionAttribute.Constellation -> SolarSystemChipLocation.Constellation(value.constellation.id)
                is OpportunityContributionAttribute.Region -> SolarSystemChipLocation.Region(value.region.id)
                is OpportunityContributionAttribute.Station -> value.solarSystem?.id?.let { SolarSystemChipLocation.SolarSystem(it) }
                is OpportunityContributionAttribute.Structure -> value.solarSystem?.id?.let { SolarSystemChipLocation.SolarSystem(it) }
                else -> null
            }
        }
        val broadcastLocations = broadcastLocations?.map {
            SolarSystemChipLocation.SolarSystem(it.id.toInt())
        }
        val solarSystemChipLocations = attributeLocations + broadcastLocations.orEmpty()
        if (solarSystemChipLocations.isEmpty()) return null
        return getSolarSystemChipStateUseCase(solarSystemChipLocations)
    }

    fun uuidToInt128(uuid: String): String {
        val uuid = UUID.fromString(uuid)
        val mostSigBits = uuid.mostSignificantBits
        val leastSigBits = uuid.leastSignificantBits

        // Create an unsigned mask (2^64 - 1 using BigInteger)
        @Suppress("SpellCheckingInspection")
        val mask = BigInteger("FFFFFFFFFFFFFFFF", 16)

        // Apply the mask to convert signed long to unsigned BigInteger
        val high = BigInteger.valueOf(mostSigBits).and(mask)
        val low = BigInteger.valueOf(leastSigBits).and(mask)

        // Combine the high and low bits into a 128-bit integer
        return high.shiftLeft(64).or(low).toString()
    }

    fun int128ToUuid(int128: String): String {
        // Parse the 128-bit integer from the string
        val bigInt = BigInteger(int128)

        // Split into the most significant and least significant bits
        @Suppress("SpellCheckingInspection")
        val mask = BigInteger("FFFFFFFFFFFFFFFF", 16)
        val leastSigBits = bigInt.and(mask).toLong() // Extract lower 64 bits
        val mostSigBits = bigInt.shiftRight(64).and(mask).toLong() // Extract upper 64 bits

        // Create the UUID from the most and least significant bits
        return UUID(mostSigBits, leastSigBits).toString()
    }
}
