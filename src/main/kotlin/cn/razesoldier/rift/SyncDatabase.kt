package cn.razesoldier.rift

import dev.nohus.rift.database.static.Celestials
import dev.nohus.rift.database.static.Constellations
import dev.nohus.rift.database.static.Planets
import dev.nohus.rift.database.static.Regions
import dev.nohus.rift.database.static.SolarSystems
import dev.nohus.rift.database.static.StarGates
import dev.nohus.rift.database.static.TypeGroups
import dev.nohus.rift.database.static.Types
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.File

/**
 * 用于同步英文数据库的星系、星座和星域
 */
val sdePath = File(System.getenv("SDE_PATH"))
val sdeReader = SDEReader(sdePath)

fun main() {
    syncSolarSystems()
    syncConstellations()
    syncRegions()
    syncStarGates()
    syncPlanets()
    syncTypeGroups()
    syncCelestials()
}

private fun syncSolarSystems() {
    transaction(StaticDatabase.zhDb) {
        exec("ATTACH DATABASE 'src/main/resources/static.db' AS en")
        SolarSystems.deleteAll()
        exec("INSERT INTO SolarSystems SELECT * FROM en.SolarSystems")
        sdeReader.readSolarSystems().forEachLine { line ->
            val system = Json.decodeFromString<Entity>(line)
            if (!system.name["en"].equals(system.name["zh"])) {
                SolarSystems.update({ SolarSystems.solarSystemId eq system._key }) {
                    it[solarSystemName] = system.name.getValue("zh")
                }
            }
        }
    }
}

private fun syncConstellations() {
    transaction(StaticDatabase.zhDb) {
        exec("ATTACH DATABASE 'src/main/resources/static.db' AS en")
        Constellations.deleteAll()
        exec("INSERT INTO Constellations SELECT * FROM en.Constellations")
        sdeReader.readConstellations().forEachLine { line ->
            val constellation = Json.decodeFromString<Entity>(line)
            if (!constellation.name["en"].equals(constellation.name["zh"])) {
                Constellations.update({ Constellations.constellationId eq constellation._key }) {
                    it[constellationName] = constellation.name.getValue("zh")
                }
            }
        }
    }
}

private fun syncRegions() {
    transaction(StaticDatabase.zhDb) {
        exec("ATTACH DATABASE 'src/main/resources/static.db' AS en")
        Regions.deleteAll()
        exec("INSERT INTO Regions SELECT * FROM en.Regions")
        sdeReader.readRegions().forEachLine { line ->
            val region = Json.decodeFromString<Entity>(line)
            if (!region.name["en"].equals(region.name["zh"])) {
                Regions.update({ Regions.regionId eq region._key }) {
                    it[regionName] = region.name.getValue("zh")
                }
            }
        }
    }
}

private fun syncStarGates() {
    transaction(StaticDatabase.zhDb) {
        exec("ATTACH DATABASE 'src/main/resources/static.db' AS en")
        StarGates.deleteAll()
        exec("INSERT INTO StarGates SELECT * FROM en.StarGates")
    }
}

private fun syncPlanets() {
    transaction(StaticDatabase.zhDb) {
        exec("ATTACH DATABASE 'src/main/resources/static.db' AS en")
        Planets.deleteAll()
        exec("INSERT INTO Planets SELECT * FROM en.Planets")
        sdeReader.readPlanets().forEachLine { line ->
            val planet = Json.decodeFromString<Planet>(line)
            Planets.update({ Planets.id eq planet._key }) {
                it[name] = composePlanet(planet.solarSystemID, planet.celestialIndex)
            }
        }
    }
}

private fun syncTypeGroups() {
    transaction(StaticDatabase.zhDb) {
        exec("ATTACH DATABASE 'src/main/resources/static.db' AS en")
        TypeGroups.deleteAll()
        exec("INSERT INTO TypeGroups SELECT * FROM en.TypeGroups")
        val groups = mutableMapOf<Int, Entity>()
        sdeReader.readTypeGroups().forEachLine { line ->
            val group = Json.decodeFromString<Entity>(line)
            groups[group._key] = group
        }
        TypeGroups.selectAll().forEach { row ->
            val groupId = row[TypeGroups.groupId]
            TypeGroups.update({ TypeGroups.groupId eq groupId }) {
                it[TypeGroups.groupName] = groups[groupId]!!.name.getValue("zh")
            }
        }
    }
}

private fun syncCelestials() {
    transaction(StaticDatabase.zhDb) {
        exec("ATTACH DATABASE 'src/main/resources/static.db' AS en")
        Celestials.deleteAll()
        exec("INSERT INTO Celestials SELECT * FROM en.Celestials")
    }
    val pageSize = 1000
    var lastId = 0

    while (true) {
        val celestials = transaction(StaticDatabase.zhDb) {
            Celestials.selectAll()
                .where { Celestials.id greater lastId }
                .orderBy(Celestials.id to SortOrder.ASC)
                .limit(pageSize)
                .toList()
        }
        if (celestials.isEmpty()) break

        celestials.forEach { celestial ->
            val id = celestial[Celestials.id]
            val typeId = celestial[Celestials.typeId]
            if (id !in 40_000_000..<50_000_000) {
                return
            }
            transaction(StaticDatabase.zhDb) {
                val type = Types.select(Types.groupId, Types.typeId)
                    .where { Types.typeId eq typeId }
                    .single()
                if (type[Types.groupId] == 6) {
                    // Sun
                    val name = SolarSystems.select(SolarSystems.solarSystemName)
                        .where { SolarSystems.solarSystemId eq celestial[Celestials.solarSystemId] }
                        .single()[SolarSystems.solarSystemName]
                    updateCelestialName(id, name)
                }
                if (type[Types.groupId] == 7) {
                    // Planet
                    val name = Planets.select(Planets.name)
                        .where { Planets.id eq id }
                        .single()[Planets.name]
                    updateCelestialName(id, name)
                }
                if (type[Types.typeId] == 14) {
                    // Moon
                    val regex = Regex("""Moon (\d{1,2})""")
                    val moonNumber = regex.find(celestial[Celestials.name])?.groupValues[1]
                    if (moonNumber == null) {
                        val name = when (celestial[Celestials.id]) {
                            40319255 -> "柯埃佐首星 IV（伊克里普）- 格里卡拉卫星"
                            40319256 -> "柯埃佐首星 IV（伊克里普）- 黑奎卫星"
                            40319257 -> "柯埃佐首星 IV（伊克里普）- 凯利尔库卫星"
                            else -> "未知卫星"
                        }
                        updateCelestialName(id, name)
                        return@transaction
                    }
                    val planetName = Planets.select(Planets.name)
                        .where { Planets.id eq celestial[Celestials.orbitId]!! }
                        .single()[Planets.name]
                    updateCelestialName(id, "$planetName - 卫星 $moonNumber")
                }
            }
        }

        lastId = celestials.last()[Celestials.id]
    }
}

private fun updateCelestialName(celestialId: Int, name: String) {
    Celestials.update({ Celestials.id eq celestialId } ) {
        it[Celestials.name] = name
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
private data class Entity(
    val _key: Int,
    val name: Map<String, String>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
private data class Planet(val _key: Int, val solarSystemID: Int, val celestialIndex: Int)