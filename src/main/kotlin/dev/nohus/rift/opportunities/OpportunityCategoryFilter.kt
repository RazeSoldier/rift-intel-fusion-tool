package dev.nohus.rift.opportunities

import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.*
import dev.nohus.rift.i18n.getStringSync
import org.jetbrains.compose.resources.DrawableResource

sealed class OpportunityCategoryFilterType(val name: String) {
    data object Feature : OpportunityCategoryFilterType("Feature")
    data object CareerPath : OpportunityCategoryFilterType("Career Path")
    data object Activity : OpportunityCategoryFilterType("Activity")
}

sealed class OpportunityCategoryFilter(
    val order: Int,
    val name: String,
    val type: OpportunityCategoryFilterType,
    val description: String,
    val icon: DrawableResource? = null,
) {
    data object CorporationProjects : OpportunityCategoryFilter(
        order = 0,
        name = getStringSync(Res.string.opportunities_window_corp_projects),
        type = OpportunityCategoryFilterType.Feature,
        description = getStringSync(Res.string.opportunities_window_corp_projects_description),
        icon = Res.drawable.corporation_management_16px,
    )

    data object FactionalWarfare : OpportunityCategoryFilter(
        order = 1,
        name = getStringSync(Res.string.opportunities_window_factional_warfare),
        type = OpportunityCategoryFilterType.Feature,
        description = getStringSync(Res.string.opportunities_window_factional_warfare_description),
        icon = Res.drawable.flag_16px,
    )

    data object FreelanceJobs : OpportunityCategoryFilter(
        order = 2,
        name = getStringSync(Res.string.opportunities_window_freelance_jobs),
        type = OpportunityCategoryFilterType.Feature,
        description = getStringSync(Res.string.opportunities_window_freelance_jobs_description),
        icon = Res.drawable.freelance_projects_16px,
    )

    data object Enforcer : OpportunityCategoryFilter(
        order = 3,
        name = getStringSync(Res.string.opportunities_window_enforecer),
        type = OpportunityCategoryFilterType.CareerPath,
        description = getStringSync(Res.string.opportunities_window_enforecer_description),
        icon = Res.drawable.careerpaths_enforcer_16px,
    )

    data object Explorer : OpportunityCategoryFilter(
        order = 4,
        name = getStringSync(Res.string.opportunities_window_explorer),
        type = OpportunityCategoryFilterType.CareerPath,
        description = getStringSync(Res.string.opportunities_window_explorer_description),
        icon = Res.drawable.careerpaths_explorer_16px,
    )

    data object Industrialist : OpportunityCategoryFilter(
        order = 5,
        name = getStringSync(Res.string.opportunities_window_industrialist),
        type = OpportunityCategoryFilterType.CareerPath,
        description = getStringSync(Res.string.opportunities_window_industrialist_description),
        icon = Res.drawable.careerpaths_industrialist_16px,
    )

    data object SoldierOfFortune : OpportunityCategoryFilter(
        order = 6,
        name = getStringSync(Res.string.opportunities_window_soldier_of_fortune),
        type = OpportunityCategoryFilterType.CareerPath,
        description = getStringSync(Res.string.opportunities_window_soldier_of_fortune_description),
        icon = Res.drawable.careerpaths_soldier_of_fortune_16px,
    )

    data object Combat : OpportunityCategoryFilter(
        order = 7,
        name = getStringSync(Res.string.opportunities_window_combat),
        type = OpportunityCategoryFilterType.Activity,
        description = getStringSync(Res.string.opportunities_window_combat_description),
        icon = Res.drawable.sword_16px,
    )

    data object CosmicSignatures : OpportunityCategoryFilter(
        order = 8,
        name = getStringSync(Res.string.opportunities_window_cosmic_signatures),
        type = OpportunityCategoryFilterType.Activity,
        description = getStringSync(Res.string.opportunities_window_cosmic_signatures_description),
        icon = Res.drawable.pinpoint_probe_formation_32px,
    )

    data object Fleet : OpportunityCategoryFilter(
        order = 9,
        name = getStringSync(Res.string.opportunities_window_fleet),
        type = OpportunityCategoryFilterType.Activity,
        description = getStringSync(Res.string.opportunities_window_fleet_description),
    )

    data object Hauling : OpportunityCategoryFilter(
        order = 10,
        name = getStringSync(Res.string.opportunities_window_hauling),
        type = OpportunityCategoryFilterType.Activity,
        description = getStringSync(Res.string.opportunities_window_hauling_description),
    )

    data object Logistics : OpportunityCategoryFilter(
        order = 11,
        name = getStringSync(Res.string.opportunities_window_logistics),
        type = OpportunityCategoryFilterType.Activity,
        description = getStringSync(Res.string.opportunities_window_logistics_description),
    )

    data object Manufacturing : OpportunityCategoryFilter(
        order = 12,
        name = getStringSync(Res.string.opportunities_window_manufacturing),
        type = OpportunityCategoryFilterType.Activity,
        description = getStringSync(Res.string.opportunities_window_manufacturing_description),
    )

    data object Mining : OpportunityCategoryFilter(
        order = 13,
        name = getStringSync(Res.string.opportunities_window_mining),
        type = OpportunityCategoryFilterType.Activity,
        description = getStringSync(Res.string.opportunities_window_mining_description),
        icon = Res.drawable.mining_16px,
    )
}
