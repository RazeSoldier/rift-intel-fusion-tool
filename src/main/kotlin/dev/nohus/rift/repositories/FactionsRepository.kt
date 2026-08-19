package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.Factions
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
class FactionsRepository(
    staticDatabase: StaticDatabase,
) {

    private val scope = CoroutineScope(Job())
    private lateinit var factionById: Map<Int, String>
    private val hasLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            factionById = staticDatabase.transaction {
                Factions.selectAll().associate {
                    it[Factions.id] to it[Factions.name]
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

    fun getFaction(id: Int): String? {
        blockUntilLoaded()
        return factionById[id]
    }
}
