package dev.nohus.rift.opportunities

import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.database.static.TypeLists
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class TypeListsRepository(
    staticDatabase: StaticDatabase,
) {

    data class TypeList(
        val id: Int,
        val name: String,
        val includedCategoryIDs: List<Int>?,
        val includedGroupIDs: List<Int>?,
        val includedTypeIDs: List<Int>?,
        val excludedCategoryIDs: List<Int>?,
        val excludedGroupIDs: List<Int>?,
        val excludedTypeIDs: List<Int>?,
    )

    private val scope = CoroutineScope(Job())
    private lateinit var typeListsById: Map<Int, TypeList>
    private val hasLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            val typeLists = staticDatabase.transaction {
                TypeLists.selectAll().toList()
            }.map {
                TypeList(
                    id = it[TypeLists.id],
                    name = it[TypeLists.name],
                    includedCategoryIDs = it[TypeLists.includedCategoryIDs]?.splitInts(),
                    includedGroupIDs = it[TypeLists.includedGroupIDs]?.splitInts(),
                    includedTypeIDs = it[TypeLists.includedTypeIDs]?.splitInts(),
                    excludedCategoryIDs = it[TypeLists.excludedCategoryIDs]?.splitInts(),
                    excludedGroupIDs = it[TypeLists.excludedGroupIDs]?.splitInts(),
                    excludedTypeIDs = it[TypeLists.excludedTypeIDs]?.splitInts(),
                )
            }
            typeListsById = typeLists.associateBy { it.id }
            hasLoaded.complete(Unit)
        }
    }

    private fun String.splitInts(): List<Int> {
        return split(",").map { it.toInt() }
    }

    private fun blockUntilLoaded() {
        runBlocking {
            hasLoaded.await()
        }
    }

    fun getTypeList(id: Int): TypeList? {
        blockUntilLoaded()
        return typeListsById[id]
    }
}
