package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.database.static.MetaGroups
import dev.nohus.rift.database.static.TypeCategories
import dev.nohus.rift.database.static.TypeDogmas
import dev.nohus.rift.database.static.TypeGroups
import dev.nohus.rift.database.static.Types
import dev.nohus.rift.network.requests.Originator
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
    private val namesRepository: NamesRepository,
) {

    data class Type(
        val id: Int,
        val groupId: Int,
        val categoryId: Int,
        val name: String,
        val volume: Float,
        val radius: Float?,
        val repackagedVolume: Float?,
        val iconId: Int,
        val metaGroupId: Int?,
        val metaLevel: Int?,
        val techLevel: Int?,
        val dogmas: Dogmas,
    )

    data class Dogmas(
        val entityOverviewShipGroupId: Int?,
    )

    data class TypeCategory(
        val id: Int,
        val name: String,
        val isPublished: Boolean,
    )

    data class TypeGroup(
        val id: Int,
        val categoryId: Int,
        val name: String,
        val isPublished: Boolean,
    )

    data class MetaGroup(
        val id: Int,
        val name: String,
    )

    private val scope = CoroutineScope(Job())

    /**
     * Names resolved from ESI for types not in the SDE
     */
    private lateinit var types: Map<Int, Type>
    private lateinit var typeIds: Map<String, Int>
    private lateinit var groups: Map<Int, TypeGroup>
    private lateinit var groupTypes: Map<Int, List<Type>>
    private lateinit var categories: Map<Int, TypeCategory>
    private lateinit var categoryTypes: Map<Int, List<Type>>
    private lateinit var metaGroups: Map<Int, MetaGroup>
    private val hasLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            val rows = staticDatabase.transaction {
                Types.selectAll().toList()
            }
            val dogmaRows = staticDatabase.transaction {
                TypeDogmas.selectAll().toList()
            }.associateBy { it[TypeDogmas.typeId] }
            metaGroups = staticDatabase.transaction {
                MetaGroups.selectAll().toList()
            }.associate {
                it[MetaGroups.metaGroupId] to MetaGroup(it[MetaGroups.metaGroupId], it[MetaGroups.metaGroupName])
            }
            types = rows.associate {
                val id = it[Types.typeId]
                id to Type(
                    id = id,
                    groupId = it[Types.groupId],
                    categoryId = it[Types.categoryId],
                    name = it[Types.typeName],
                    volume = it[Types.volume],
                    radius = it[Types.radius],
                    repackagedVolume = it[Types.repackagedVolume],
                    iconId = it[Types.iconId] ?: it[Types.typeId],
                    metaGroupId = it[Types.metaGroupId],
                    metaLevel = it[Types.metaLevel],
                    techLevel = it[Types.techLevel],
                    dogmas = Dogmas(
                        entityOverviewShipGroupId = dogmaRows[id]?.get(TypeDogmas.entityOverviewShipGroupId),
                    ),
                )
            }
            val groupRows = staticDatabase.transaction {
                TypeGroups.selectAll().toList()
            }
            groups = groupRows.associate {
                it[TypeGroups.groupId] to TypeGroup(it[TypeGroups.groupId], it[TypeGroups.categoryId], it[TypeGroups.groupName], it[TypeGroups.published])
            }
            groupTypes = types.values.groupBy { it.groupId }
            val categoryRows = staticDatabase.transaction {
                TypeCategories.selectAll().toList()
            }
            categories = categoryRows.associate {
                it[TypeCategories.categoryId] to TypeCategory(it[TypeCategories.categoryId], it[TypeCategories.categoryName], it[TypeCategories.published])
            }
            categoryTypes = types.values.groupBy { it.categoryId }
            typeIds = rows.groupBy { it[Types.typeName] }.map { (name, rows) ->
                name to if (rows.size == 1) {
                    rows.single()[Types.typeId]
                } else {
                    // Duplicate type names
                    rows.maxByOrNull {
                        // Prefer ships
                        it[Types.categoryId] in listOf(6)
                    }!![Types.typeId]
                }
            }.toMap()
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
        return getType(id)?.name ?: namesRepository.getName(id)
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
            name = namesRepository.getName(id) ?: "Unknown",
            volume = 0f,
            radius = null,
            repackagedVolume = null,
            iconId = -1,
            metaGroupId = null,
            metaLevel = null,
            techLevel = null,
            dogmas = Dogmas(null),
        )
    }

    fun getTypesInGroup(groupId: Int): List<Type> {
        return groupTypes[groupId] ?: listOf()
    }

    fun getTypesInCategory(categoryId: Int): List<Type> {
        return categoryTypes[categoryId] ?: listOf()
    }

    fun getCategories(): List<TypeCategory> {
        blockUntilLoaded()
        return categories.values.sortedBy { it.name }
    }

    fun getGroupsInCategory(categoryId: Int): List<TypeGroup> {
        blockUntilLoaded()
        return groups.values.filter { it.categoryId == categoryId }.sortedBy { it.name }
    }

    fun getGroupName(id: Int): String? {
        blockUntilLoaded()
        return groups[id]?.name
    }

    fun getCategoryName(id: Int): String? {
        blockUntilLoaded()
        return categories[id]?.name
    }

    fun getMetaGroups(): List<MetaGroup> {
        blockUntilLoaded()
        return metaGroups.values.sortedBy { it.id }
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

    suspend fun resolveNamesFromEsi(originator: Originator, ids: List<Int>) {
        namesRepository.resolveNames(originator, ids)
    }
}
