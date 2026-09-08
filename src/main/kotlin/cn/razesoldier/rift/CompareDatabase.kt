package cn.razesoldier.rift

import dev.nohus.rift.database.static.SolarSystems
import dev.nohus.rift.database.static.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.collections.listOf

val TABLES = listOf(
    SolarSystems,
    Regions,
    Constellations,
    MapLayouts,
    MapLayout,
    RegionMapLayout,
    Ships,
    Types,
    TypeGroups,
    TypeCategories,
    StarGates,
    Stations,
    Planets,
    PlanetaryIndustrySchematics,
    PlanetaryIndustrySchematicsTypes,
    Celestials,
    TypeDogmas,
    Backdrops,
)

fun main() {
    val dbEn = StaticDatabase.enDb
    val dbZh = StaticDatabase.zhDb
    val results = CompareDatabase(dbEn, dbZh, TABLES).compare()
    for (result in results) {
        if (result.diff != 0L) {
            println(result)
        }
    }
}

/**
 * 表行数比较结果
 * @param tableNameA 数据库A中的表名
 * @param tableNameB 数据库B中的对应表名
 * @param rowCountA 数据库A中该表的行数
 * @param rowCountB 数据库B中该表的行数
 * @param diff 行数差异（rowCountA - rowCountB）
 */
data class TableCompareResult(
    val tableNameA: String,
    val tableNameB: String,
    val rowCountA: Long,
    val rowCountB: Long,
    val diff: Long = rowCountA - rowCountB,
)

/**
 * 对比两个数据库中对应表的行数差异（基于 Exposed ORM，不使用 JDBC）
 *
 * 使用方式：
 * ```kotlin
 * val dbA = Database.connect("jdbc:sqlite:/path/to/dbA.db", "org.sqlite.JDBC")
 * val dbB = Database.connect("jdbc:sqlite:/path/to/dbB.db", "org.sqlite.JDBC")
 * val tableMapping = mapOf("users" to "t_users", "orders" to "t_orders")
 * val comparator = CompareDatabase(dbA, dbB, tableMapping)
 * val results = comparator.compare()
 * results.forEach {
 *     println("${it.tableNameA} vs ${it.tableNameB}: ${it.rowCountA} vs ${it.rowCountB}, diff=${it.diff}")
 * }
 * ```
 *
 * @param databaseA 数据库A的 Exposed Database 实例
 * @param databaseB 数据库B的 Exposed Database 实例
 */
class CompareDatabase(
    private val databaseA: Database,
    private val databaseB: Database,
    private val tables: List<Table>,
) {
    /**
     * 执行对比，返回每个表对的行数比较结果
     */
    fun compare(): List<TableCompareResult> {
        return tables.map {
            val countA = getRowCount(databaseA, it)
            val countB = getRowCount(databaseB, it)
            TableCompareResult(it::class.simpleName!!, it::class.simpleName!!, countA, countB)
        }
    }

    private fun getRowCount(database: Database, table: Table): Long {
        return transaction(database) { table.selectAll().count() }
    }
}
