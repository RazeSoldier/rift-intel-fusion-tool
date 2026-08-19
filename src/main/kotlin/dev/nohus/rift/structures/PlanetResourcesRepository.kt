package dev.nohus.rift.structures

import dev.nohus.rift.database.static.PlanetResources
import dev.nohus.rift.database.static.StaticDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class PlanetResourcesRepository(
    staticDatabase: StaticDatabase,
) {

    sealed interface PlanetResource {
        data class Power(val power: Int) : PlanetResource
        data class Workforce(val workforce: Int) : PlanetResource
        data class Reagent(
            val typeId: Int,
            val amountPerCycle: Int,
            val cyclePeriod: Int,
            val securedCapacity: Int,
            val unsecuredCapacity: Int,
        ) : PlanetResource
    }

    private val scope = CoroutineScope(Job())
    private lateinit var resourcesById: Map<Int, PlanetResource>
    private val hasLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            resourcesById = staticDatabase.transaction {
                PlanetResources.selectAll().toList()
            }.associate {
                val id = it[PlanetResources.id]
                val power = it[PlanetResources.power]
                val workforce = it[PlanetResources.workforce]
                val reagentTypeId = it[PlanetResources.reagentTypeId]
                val reagentAmountPerCycle = it[PlanetResources.reagentAmountPerCycle]
                val reagentCyclePeriod = it[PlanetResources.reagentCyclePeriod]
                val reagentSecuredCapacity = it[PlanetResources.reagentSecuredCapacity]
                val reagentUnsecuredCapacity = it[PlanetResources.reagentUnsecuredCapacity]

                id to if (power != null) {
                    PlanetResource.Power(power)
                } else if (workforce != null) {
                    PlanetResource.Workforce(workforce)
                } else if (
                    reagentTypeId != null &&
                    reagentAmountPerCycle != null &&
                    reagentCyclePeriod != null &&
                    reagentSecuredCapacity != null &&
                    reagentUnsecuredCapacity != null
                ) {
                    PlanetResource.Reagent(
                        reagentTypeId,
                        reagentAmountPerCycle,
                        reagentCyclePeriod,
                        reagentSecuredCapacity,
                        reagentUnsecuredCapacity,
                    )
                } else {
                    throw IllegalStateException("Invalid PlanetResource data")
                }
            }
            hasLoaded.complete(Unit)
        }
    }

    private fun blockUntilLoaded() {
        runBlocking {
            hasLoaded.await()
        }
    }

    fun get(id: Int): PlanetResource? {
        blockUntilLoaded()
        return resourcesById[id]
    }
}
