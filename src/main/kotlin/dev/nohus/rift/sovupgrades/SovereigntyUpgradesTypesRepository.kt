package dev.nohus.rift.sovupgrades

import dev.nohus.rift.database.static.SovereigntyUpgrades
import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.sovupgrades.SovereigntyUpgradesTypesRepository.SovereigntyUpgradeType.Fuel
import dev.nohus.rift.structures.SovereigntyReagent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class SovereigntyUpgradesTypesRepository(
    staticDatabase: StaticDatabase,
) {

    data class SovereigntyUpgradeType(
        val id: Int,
        val fuel: Fuel?,
        val mutuallyExclusiveGroup: String,
        val powerConsumption: Int,
        val workforceConsumption: Int,
    ) {
        data class Fuel(
            val type: SovereigntyReagent,
            val hourlyUpkeep: Int,
            val startupCost: Int,
        )
    }

    private val scope = CoroutineScope(Job())
    private lateinit var upgradesById: Map<Int, SovereigntyUpgradeType>
    private val hasLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            upgradesById = staticDatabase.transaction {
                SovereigntyUpgrades.selectAll().toList()
            }.map {
                SovereigntyUpgradeType(
                    id = it[SovereigntyUpgrades.id],
                    fuel = it[SovereigntyUpgrades.fuelTypeId]?.let { typeId ->
                        Fuel(
                            type = when (typeId) {
                                81144 -> SovereigntyReagent.SuperionicIce
                                81143 -> SovereigntyReagent.MagmaticGas
                                else -> throw IllegalStateException("Unknown resource type $typeId")
                            },
                            hourlyUpkeep = it[SovereigntyUpgrades.fuelHourlyUpkeep]!!,
                            startupCost = it[SovereigntyUpgrades.fuelStartupCost]!!,
                        )
                    },
                    mutuallyExclusiveGroup = it[SovereigntyUpgrades.mutuallyExclusiveGroup],
                    powerConsumption = it[SovereigntyUpgrades.powerAllocation] ?: it[SovereigntyUpgrades.powerProduction]?.let { -it } ?: 0,
                    workforceConsumption = it[SovereigntyUpgrades.workforceAllocation] ?: it[SovereigntyUpgrades.workforceProduction]?.let { -it } ?: 0,
                )
            }.associateBy {
                it.id
            }
            hasLoaded.complete(Unit)
        }
    }

    private fun blockUntilLoaded() {
        runBlocking {
            hasLoaded.await()
        }
    }

    fun get(id: Int): SovereigntyUpgradeType? {
        blockUntilLoaded()
        return upgradesById[id]
    }
}
