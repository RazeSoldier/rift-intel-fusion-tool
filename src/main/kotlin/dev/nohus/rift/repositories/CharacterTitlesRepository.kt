package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.CharacterTitles
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
class CharacterTitlesRepository(
    staticDatabase: StaticDatabase,
) {

    private val scope = CoroutineScope(Job())
    private lateinit var titleById: Map<String, String>
    private val hasLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            titleById = staticDatabase.transaction {
                CharacterTitles.selectAll().associate {
                    it[CharacterTitles.id] to it[CharacterTitles.name]
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

    fun getTitle(id: String): String? {
        blockUntilLoaded()
        return titleById[id]
    }
}
