package dev.nohus.rift.corpprojects

import androidx.compose.ui.text.AnnotatedString
import dev.nohus.rift.compose.RiftOpportunityCardCategory
import dev.nohus.rift.compose.RiftOpportunityCardType
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.contribution_method_attack_fw_complex_16px
import dev.nohus.rift.generated.resources.contribution_method_damage_ship_16px
import dev.nohus.rift.generated.resources.contribution_method_defend_fw_complex_16px
import dev.nohus.rift.generated.resources.contribution_method_deliver_item_16px
import dev.nohus.rift.generated.resources.contribution_method_destroy_npc_16px
import dev.nohus.rift.generated.resources.contribution_method_destroy_ship_16px
import dev.nohus.rift.generated.resources.contribution_method_earn_loyalty_point_16px
import dev.nohus.rift.generated.resources.contribution_method_lost_ship_16px
import dev.nohus.rift.generated.resources.contribution_method_manual_16px
import dev.nohus.rift.generated.resources.contribution_method_manufacture_item_16px
import dev.nohus.rift.generated.resources.contribution_method_mine_material_16px
import dev.nohus.rift.generated.resources.contribution_method_remote_boost_shields_16px
import dev.nohus.rift.generated.resources.contribution_method_remote_repair_armor_16px
import dev.nohus.rift.generated.resources.contribution_method_salvage_wreck_16px
import dev.nohus.rift.generated.resources.contribution_method_scan_signatures_16px
import dev.nohus.rift.generated.resources.contribution_method_ship_insurance_16px
import dev.nohus.rift.network.esi.models.CorporationProjectCareer
import org.jetbrains.compose.resources.DrawableResource

object CorporationProjectsUtils {

    fun getProjectCategory(project: Project): RiftOpportunityCardCategory {
        val category = when (project.details.career) {
            CorporationProjectCareer.Unspecified -> RiftOpportunityCardCategory.Unclassified
            CorporationProjectCareer.Explorer -> RiftOpportunityCardCategory.Explorer
            CorporationProjectCareer.Industrialist -> RiftOpportunityCardCategory.Industrialist
            CorporationProjectCareer.Enforcer -> RiftOpportunityCardCategory.Enforcer
            CorporationProjectCareer.SoldierOfFortune -> RiftOpportunityCardCategory.SoldierOfFortune
        }
        return category
    }

    data class ProjectCategoryMetadata(
        val name: String,
        val icon: DrawableResource? = null,
        val tooltip: String? = null,
        val progressUnit: String?,
        val rewardPer: String?,
    )

    fun getProjectType(project: Project): RiftOpportunityCardType {
        val metadata = getProjectTypeMetadata(project)
        return RiftOpportunityCardType(
            text = AnnotatedString(metadata.name),
            icon = metadata.icon,
            tooltip = metadata.tooltip,
        )
    }

    fun getProjectTypeMetadata(project: Project): ProjectCategoryMetadata {
        return when (val configuration = project.details.configuration) {
            is ProjectConfiguration.CaptureFwComplex -> ProjectCategoryMetadata(
                name = "Capture Factional Warfare Complexes",
                icon = Res.drawable.contribution_method_attack_fw_complex_16px,
                tooltip = "Capture complexes for your militia in Factional Warfare.",
                progressUnit = "Complexes captured",
                rewardPer = "complex captured",
            )

            is ProjectConfiguration.DamageShip -> ProjectCategoryMetadata(
                name = "Damage Capsuleers",
                icon = Res.drawable.contribution_method_damage_ship_16px,
                tooltip = "Damage ships piloted by capsuleers.",
                progressUnit = "Damage dealt",
                rewardPer = "point of damage",
            )

            is ProjectConfiguration.DefendFwComplex -> ProjectCategoryMetadata(
                name = "Defend Factional Warfare Complexes",
                icon = Res.drawable.contribution_method_defend_fw_complex_16px,
                tooltip = "Defend complexes for your militia in Factional Warfare.",
                progressUnit = "Complexes defended",
                rewardPer = "complex defended",
            )

            is ProjectConfiguration.DeliverItem -> ProjectCategoryMetadata(
                name = "Deliver",
                icon = Res.drawable.contribution_method_deliver_item_16px,
                tooltip = "Deliver items of a specific type\nto the Projects hangar in a\ncorporation office.",
                progressUnit = "Items delivered",
                rewardPer = "item delivered",
            )

            is ProjectConfiguration.DestroyNpc -> ProjectCategoryMetadata(
                name = "Destroy Non-Capsuleers",
                icon = Res.drawable.contribution_method_destroy_npc_16px,
                tooltip = "Destroy ships piloted by non-capsuleers.",
                progressUnit = "Non-capsuleers destroyed",
                rewardPer = "kill",
            )

            is ProjectConfiguration.DestroyShip -> ProjectCategoryMetadata(
                name = "Destroy Capsuleer's Ship",
                icon = Res.drawable.contribution_method_destroy_ship_16px,
                tooltip = "Destroy ships piloted by capsuleers.",
                progressUnit = "Capsuleers destroyed",
                rewardPer = "kill",
            )

            is ProjectConfiguration.EarnLoyaltyPoint -> ProjectCategoryMetadata(
                name = "Earn Loyalty Points",
                icon = Res.drawable.contribution_method_earn_loyalty_point_16px,
                tooltip = "Earn Loyalty Points from either any source or a specific corporation.",
                progressUnit = "Loyalty points earned",
                rewardPer = "loyalty point earned",
            )

            is ProjectConfiguration.LostShip -> ProjectCategoryMetadata(
                name = "Ship Loss to Capsuleers",
                icon = Res.drawable.contribution_method_lost_ship_16px,
                tooltip = "Receive a corporation ISK payout for losing a ship.",
                progressUnit = "ships lost",
                rewardPer = "ship lost",
            )

            ProjectConfiguration.Manual -> ProjectCategoryMetadata(
                name = "Manual",
                icon = Res.drawable.contribution_method_manual_16px,
                tooltip = "Manually update progress for any participation or measurement that can't be automatically tracked.",
                progressUnit = null,
                rewardPer = "unit of progress",
            )

            is ProjectConfiguration.ManufactureItem -> ProjectCategoryMetadata(
                name = "Manufacture",
                icon = Res.drawable.contribution_method_manufacture_item_16px,
                tooltip = "Install manufacturing jobs for items of a specific type.",
                progressUnit = "Items manufactured",
                rewardPer = "item manufactured",
            )

            is ProjectConfiguration.MineMaterial -> ProjectCategoryMetadata(
                name = "Mine Materials",
                icon = Res.drawable.contribution_method_mine_material_16px,
                tooltip = "Mine raw materials.",
                progressUnit = "Materials mined",
                rewardPer = "unit",
            )

            is ProjectConfiguration.RemoteBoostShield -> ProjectCategoryMetadata(
                name = "Remote Boost Shield",
                icon = Res.drawable.contribution_method_remote_boost_shields_16px,
                tooltip = "Remote boost a capsuleer's shields.",
                progressUnit = "HP boosted",
                rewardPer = "hit point boosted",
            )

            is ProjectConfiguration.RemoteRepairArmor -> ProjectCategoryMetadata(
                name = "Remote Repair Armor",
                icon = Res.drawable.contribution_method_remote_repair_armor_16px,
                tooltip = "Remote repair a capsuleer's armor.",
                progressUnit = "HP repaired",
                rewardPer = "hit point repaired",
            )

            is ProjectConfiguration.SalvageWreck -> ProjectCategoryMetadata(
                name = "Salvage Wrecks",
                icon = Res.drawable.contribution_method_salvage_wreck_16px,
                tooltip = "Successfully salvage wrecks of any kind.",
                progressUnit = "Wrecks salvaged",
                rewardPer = "wreck salvaged",
            )

            is ProjectConfiguration.ScanSignature -> ProjectCategoryMetadata(
                name = "Scan Signatures",
                icon = Res.drawable.contribution_method_scan_signatures_16px,
                tooltip = "Scan cosmic signatures to 100% resolution with a probe scanner.",
                progressUnit = "Signatures scanned",
                rewardPer = "signature scanned",
            )

            is ProjectConfiguration.ShipInsurance -> ProjectCategoryMetadata(
                name = "Ship Insurance",
                icon = Res.drawable.contribution_method_ship_insurance_16px,
                tooltip = "Receive ISK compensation for losing a ship, based on the Kill Report market value of the ship and its fitting.\n\nThis project provides an additional insurance paid by the corporation for the ships lost by its members. It works in addition to the ship insurance that capsuleers can purchase themselves in stations.",
                progressUnit = "Compensated",
                rewardPer = null,
            )

            is ProjectConfiguration.Unknown -> ProjectCategoryMetadata(
                name = configuration.type,
                tooltip = "Project of an unknown type.",
                progressUnit = null,
                rewardPer = null,
            )
        }
    }
}
