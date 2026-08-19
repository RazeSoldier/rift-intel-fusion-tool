package dev.nohus.rift.repositories

import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.ship_tree_group_battlecruiser_64
import dev.nohus.rift.generated.resources.ship_tree_group_battleship_64
import dev.nohus.rift.generated.resources.ship_tree_group_capital_64
import dev.nohus.rift.generated.resources.ship_tree_group_capsule_64
import dev.nohus.rift.generated.resources.ship_tree_group_cruiser_64
import dev.nohus.rift.generated.resources.ship_tree_group_destroyer_64
import dev.nohus.rift.generated.resources.ship_tree_group_freighter_64
import dev.nohus.rift.generated.resources.ship_tree_group_frigate_64
import dev.nohus.rift.generated.resources.ship_tree_group_industrial_64
import dev.nohus.rift.generated.resources.ship_tree_group_industrialcommand_64
import dev.nohus.rift.generated.resources.ship_tree_group_miningbarge_64
import dev.nohus.rift.generated.resources.ship_tree_group_miningfrigate_64
import dev.nohus.rift.generated.resources.ship_tree_group_rookie_64
import dev.nohus.rift.generated.resources.ship_tree_group_shuttle_64
import dev.nohus.rift.generated.resources.ship_tree_group_supercapital_64
import dev.nohus.rift.generated.resources.ship_tree_group_titan_64
import org.jetbrains.compose.resources.DrawableResource

object ShipTreeGroups {

    data class Group(val id: Int, val name: String, val icon: DrawableResource)

    private val groups = listOf(
        Group(4, "Corvette", Res.drawable.ship_tree_group_rookie_64),
        Group(8, "Frigate", Res.drawable.ship_tree_group_frigate_64),
        Group(9, "Navy Frigate", Res.drawable.ship_tree_group_frigate_64),
        Group(10, "Interceptor", Res.drawable.ship_tree_group_frigate_64),
        Group(11, "Assault Frigate", Res.drawable.ship_tree_group_frigate_64),
        Group(12, "Covert Ops", Res.drawable.ship_tree_group_frigate_64),
        Group(13, "Electronic Attack Ship", Res.drawable.ship_tree_group_frigate_64),
        Group(14, "Destroyer", Res.drawable.ship_tree_group_destroyer_64),
        Group(15, "Interdictor", Res.drawable.ship_tree_group_destroyer_64),
        Group(16, "Cruiser", Res.drawable.ship_tree_group_cruiser_64),
        Group(17, "Navy Cruiser", Res.drawable.ship_tree_group_cruiser_64),
        Group(18, "Recon Ship", Res.drawable.ship_tree_group_cruiser_64),
        Group(19, "Heavy Assault Cruiser", Res.drawable.ship_tree_group_cruiser_64),
        Group(20, "Heavy Interdiction Cruiser", Res.drawable.ship_tree_group_cruiser_64),
        Group(21, "Logistics Cruisers", Res.drawable.ship_tree_group_cruiser_64),
        Group(22, "Strategic Cruiser", Res.drawable.ship_tree_group_cruiser_64),
        Group(23, "Battlecruiser", Res.drawable.ship_tree_group_battlecruiser_64),
        Group(24, "Command Ships", Res.drawable.ship_tree_group_battlecruiser_64),
        Group(25, "Navy Battlecruiser", Res.drawable.ship_tree_group_battlecruiser_64),
        Group(26, "Battleship", Res.drawable.ship_tree_group_battleship_64),
        Group(27, "Black Ops", Res.drawable.ship_tree_group_battleship_64),
        Group(28, "Marauder", Res.drawable.ship_tree_group_battleship_64),
        Group(32, "Dreadnought", Res.drawable.ship_tree_group_capital_64),
        Group(33, "Carrier", Res.drawable.ship_tree_group_supercapital_64),
        Group(34, "Titan", Res.drawable.ship_tree_group_titan_64),
        Group(35, "Shuttle", Res.drawable.ship_tree_group_shuttle_64),
        Group(36, "Hauler", Res.drawable.ship_tree_group_industrial_64),
        Group(37, "Freighter", Res.drawable.ship_tree_group_freighter_64),
        Group(38, "Jump Freighters", Res.drawable.ship_tree_group_freighter_64),
        Group(40, "Transport Ship", Res.drawable.ship_tree_group_industrial_64),
        Group(41, "Mining Frigate", Res.drawable.ship_tree_group_miningfrigate_64),
        Group(42, "Mining Barge", Res.drawable.ship_tree_group_miningbarge_64),
        Group(43, "Exhumer", Res.drawable.ship_tree_group_miningbarge_64),
        Group(44, "ORE Hauler", Res.drawable.ship_tree_group_industrial_64),
        Group(45, "Industrial Command Ship", Res.drawable.ship_tree_group_industrialcommand_64),
        Group(46, "Capital Industrial Ship", Res.drawable.ship_tree_group_freighter_64),
        Group(47, "Navy Battleship", Res.drawable.ship_tree_group_battleship_64),
        Group(48, "Expedition Frigate", Res.drawable.ship_tree_group_miningfrigate_64),
        Group(50, "Tactical Destroyer", Res.drawable.ship_tree_group_destroyer_64),
        Group(93, "Command Destroyer", Res.drawable.ship_tree_group_destroyer_64),
        Group(94, "Logistics Frigates", Res.drawable.ship_tree_group_frigate_64),
        Group(96, "Flag Cruiser", Res.drawable.ship_tree_group_cruiser_64),
        Group(2101, "Navy Destroyer", Res.drawable.ship_tree_group_destroyer_64),
        Group(2102, "Navy Dreadnought", Res.drawable.ship_tree_group_capital_64),
        Group(2104, "Lancer Dreadnought", Res.drawable.ship_tree_group_capital_64),
        Group(2107, "Capsule", Res.drawable.ship_tree_group_capsule_64),
    ).associateBy { it.id }

    operator fun get(id: Int): Group? = groups[id]

    operator fun get(name: String): Group? = groups.values.firstOrNull { it.name == name }
}
