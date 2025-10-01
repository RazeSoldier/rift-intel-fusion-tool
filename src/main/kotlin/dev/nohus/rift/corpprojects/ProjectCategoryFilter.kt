package dev.nohus.rift.corpprojects

import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.careerpaths_enforcer_16px
import dev.nohus.rift.generated.resources.careerpaths_explorer_16px
import dev.nohus.rift.generated.resources.careerpaths_industrialist_16px
import dev.nohus.rift.generated.resources.careerpaths_soldier_of_fortune_16px
import dev.nohus.rift.generated.resources.flag_16px
import dev.nohus.rift.generated.resources.mining_16px
import dev.nohus.rift.generated.resources.pinpoint_probe_formation_32px
import dev.nohus.rift.generated.resources.sword_16px
import org.jetbrains.compose.resources.DrawableResource

sealed class ProjectCategoryFilter(
    val order: Int,
    val name: String,
    val type: String,
    val description: String,
    val icon: DrawableResource? = null,
) {
    data object FactionalWarfare : ProjectCategoryFilter(
        order = 0,
        name = "Factional Warfare",
        type = "Feature",
        description = "Factional Warfare sites where you can support\nthe war effort on behalf of a faction you are\nenlisted with.",
        icon = Res.drawable.flag_16px,
    )

    data object CosmicSignatures : ProjectCategoryFilter(
        order = 1,
        name = "Cosmic Signatures",
        type = "Feature",
        description = "A site that needs to be located by probe\nscanning before you can travel to it.",
        icon = Res.drawable.pinpoint_probe_formation_32px,
    )

    data object Enforcer : ProjectCategoryFilter(
        order = 2,
        name = "Enforcer",
        type = "Career Path",
        description = "Opportunities for those focused on the\nEnforcer Career path or who are interested in\nCombat against non-capsuleers.",
        icon = Res.drawable.careerpaths_enforcer_16px,
    )

    data object Explorer : ProjectCategoryFilter(
        order = 3,
        name = "Explorer",
        type = "Career Path",
        description = "Opportunities for those focused on the\nExplorer Career path or who are interested in\nexploration, scanning, or hacking.",
        icon = Res.drawable.careerpaths_explorer_16px,
    )

    data object Industrialist : ProjectCategoryFilter(
        order = 4,
        name = "Industrialist",
        type = "Career Path",
        description = "Opportunities for those focused on the\nIndustrialist Career path or who are interested\nin resource gathering, manufacturing, or hauling.",
        icon = Res.drawable.careerpaths_industrialist_16px,
    )

    data object SoldierOfFortune : ProjectCategoryFilter(
        order = 5,
        name = "Soldier of Fortune",
        type = "Career Path",
        description = "Opportunities for those focused on the\nSoldier of Fortune Career path or who are interested\nin combat against other capsuleers.",
        icon = Res.drawable.careerpaths_soldier_of_fortune_16px,
    )

    data object Combat : ProjectCategoryFilter(
        order = 6,
        name = "Combat",
        type = "Activity",
        description = "Engaging with hostile forces.",
        icon = Res.drawable.sword_16px,
    )

    data object Fleet : ProjectCategoryFilter(
        order = 7,
        name = "Fleet",
        type = "Activity",
        description = "Form a fleet with other capsuleers to\ncooperate and complete objectives.",
    )

    data object Hauling : ProjectCategoryFilter(
        order = 8,
        name = "Hauling",
        type = "Activity",
        description = "Transporting items from location to location.",
    )

    data object Logistics : ProjectCategoryFilter(
        order = 9,
        name = "Logistics",
        type = "Activity",
        description = "Using remote modules to boost, repair, or\ntransfer energy to friendly targets.",
    )

    data object Manufacturing : ProjectCategoryFilter(
        order = 10,
        name = "Manufacturing",
        type = "Activity",
        description = "Using blueprints to produce items.",
    )

    data object Mining : ProjectCategoryFilter(
        order = 11,
        name = "Mining",
        type = "Activity",
        description = "Harvesting ore from asteroids.",
        icon = Res.drawable.mining_16px,
    )
}
