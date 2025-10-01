package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.database.static.TypeGroups
import dev.nohus.rift.database.static.Types
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class TypesRepository(
    staticDatabase: StaticDatabase,
    private val esiApi: EsiApi,
) {

    data class Type(
        val id: Int,
        val groupId: Int,
        val categoryId: Int,
        val name: String,
        val volume: Float,
        val radius: Float?,
        val repackagedVolume: Int?,
        val iconId: Int,
    )

    private val scope = CoroutineScope(Job())

    /**
     * Names resolved from ESI for types not in the SDE
     */
    private val resolvedTypeNames = mutableMapOf<Int, String>()
    private lateinit var types: Map<Int, Type>
    private lateinit var typeIds: Map<String, Int>
    private lateinit var groupNames: Map<Int, String>
    private val hasLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            val rows = staticDatabase.transaction {
                Types.selectAll().toList()
            }
            types = rows.associate {
                it[Types.typeId] to Type(
                    id = it[Types.typeId],
                    groupId = it[Types.groupId],
                    categoryId = it[Types.categoryId],
                    name = it[Types.typeName],
                    volume = it[Types.volume],
                    radius = it[Types.radius],
                    repackagedVolume = it[Types.repackagedVolume],
                    iconId = it[Types.iconId] ?: it[Types.typeId],
                )
            }
            typeIds = rows.associate { it[Types.typeName] to it[Types.typeId] }
            val groupRows = staticDatabase.transaction {
                TypeGroups.selectAll().toList()
            }
            groupNames = groupRows.associate {
                it[TypeGroups.groupId] to it[TypeGroups.groupName]
            }
            hasLoaded.complete(Unit)
        }
    }

    private fun blockUntilLoaded() {
        runBlocking {
            hasLoaded.await()
        }
    }

    private fun getTypes(): Map<Int, Type> {
        blockUntilLoaded()
        return types
    }

    private fun getTypeIds(): Map<String, Int> {
        blockUntilLoaded()
        return typeIds
    }

    fun getAllTypeNames(): List<String> {
        return getTypes().values.map { it.name }
    }

    fun getTypeId(name: String): Int? {
        return getTypeIds()[name]
    }

    fun getTypeName(id: Int): String? {
        return getType(id)?.name ?: resolvedTypeNames[id]
    }

    fun getType(name: String): Type? {
        return getTypeId(name)?.let { getType(it) }
    }

    fun getType(id: Int): Type? {
        return getTypes()[id]
    }

    fun getTypeOrPlaceholder(id: Int): Type {
        return getType(id) ?: Type(
            id = id,
            groupId = -1,
            categoryId = -1,
            name = "Unknown",
            volume = 0f,
            radius = null,
            repackagedVolume = null,
            iconId = -1,
        )
    }

    fun getGroupName(id: Int): String? {
        blockUntilLoaded()
        return groupNames[id]
    }

    /**
     * Tries to find a type name in arbitrary text
     */
    fun findTypeInText(message: String): Type? {
        return findTypesInText(message).firstOrNull()
    }

    /**
     * Tries to find type names in arbitrary text
     */
    fun findTypesInText(message: String): Sequence<Type> {
        return sequence {
            val words = message.split("[\\s,.;]+".toRegex())
            outer@for (startIndex in words.indices) {
                val longest = words.drop(startIndex).takeWhile { it.isNotEmpty() && !it[0].isLowerCase() }
                if (longest.isEmpty()) continue
                for (length in longest.size downTo 1) {
                    val candidate = longest.take(length).joinToString(" ")
                    val type = getType(candidate)
                    if (type != null) {
                        yield(type)
                        continue@outer
                    }
                }
            }
        }
    }

    suspend fun resolveNamesFromEsi(ids: List<Int>) {
        @Suppress("ConvertCallChainIntoSequence")
        resolvedTypeNames += ids
            .distinct()
            .filter { getTypeName(it) == null }
            .chunked(1000)
            .flatMap { typeIds ->
                when (val result = esiApi.postUniverseNames(typeIds)) {
                    is Result.Success -> result.data
                    is Result.Failure -> emptyList()
                }
            }
            .associate { it.id to it.name }
    }
}
