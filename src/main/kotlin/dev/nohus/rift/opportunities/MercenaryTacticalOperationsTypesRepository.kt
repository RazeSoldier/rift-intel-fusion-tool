package dev.nohus.rift.opportunities

import dev.nohus.rift.database.static.MercenaryTacticalOperations
import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.opportunities.TypeListsRepository.TypeList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class MercenaryTacticalOperationsTypesRepository(
    staticDatabase: StaticDatabase,
    typeListsRepository: TypeListsRepository,
) {

    data class MercenaryTacticalOperationType(
        val id: Int,
        val name: String,
        val description: String,
        val archetypeTitle: String,
        val archetypeDescription: String,
        val allowedShipsLists: List<TypeList>?,
        val factionId: Int?,
        val anarchyImpact: Int,
        val developmentImpact: Int,
        val infomorphBonus: Int,
        val hostiles: String,
    )

    private val scope = CoroutineScope(Job())
    private lateinit var operationsById: Map<Int, MercenaryTacticalOperationType>
    private val hasLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            val operations = staticDatabase.transaction {
                MercenaryTacticalOperations.selectAll().toList()
            }.map {
                val hostiles = when (it[MercenaryTacticalOperations.id]) {
                    12367 -> "Rogue Drones"
                    12368 -> "Drifters, Sleepers"
                    12369 -> "Sansha's Nation"
                    else -> "Unknown"
                }
                val allowedShipsLists = it[MercenaryTacticalOperations.allowedShipsList]?.split(",")?.map { it.toInt() }?.mapNotNull { typeListId ->
                    typeListsRepository.getTypeList(typeListId)
                }
                MercenaryTacticalOperationType(
                    id = it[MercenaryTacticalOperations.id],
                    name = it[MercenaryTacticalOperations.name],
                    description = it[MercenaryTacticalOperations.description],
                    archetypeTitle = it[MercenaryTacticalOperations.archetypeTitle],
                    archetypeDescription = it[MercenaryTacticalOperations.archetypeDescription],
                    allowedShipsLists = allowedShipsLists,
                    factionId = it[MercenaryTacticalOperations.factionId],
                    anarchyImpact = it[MercenaryTacticalOperations.anarchyImpact],
                    developmentImpact = it[MercenaryTacticalOperations.developmentImpact],
                    infomorphBonus = it[MercenaryTacticalOperations.infomorphBonus],
                    hostiles = hostiles,
                )
            }
            operationsById = operations.associateBy { it.id }
            hasLoaded.complete(Unit)
        }
    }

    private fun blockUntilLoaded() {
        runBlocking {
            hasLoaded.await()
        }
    }

    fun getOperation(id: Int): MercenaryTacticalOperationType? {
        blockUntilLoaded()
        return operationsById[id]
    }
}
