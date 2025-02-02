package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.Celestials
import dev.nohus.rift.database.static.StaticDatabase
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single
import kotlin.math.pow
import kotlin.math.sqrt

@Single(createdAtStart = true)
class CelestialsRepository(
    staticDatabase: StaticDatabase,
) {

    data class Celestial(
        val id: Int,
        val typeId: Int,
        val solarSystemId: Int,
        val position: Position,
        val radius: Double?,
        val name: String,
    )

    data class ClosestCelestial(
        val celestial: Celestial,
        val distance: Double,
    )

    private val celestialsBySolarSystemId: Map<Int, List<Celestial>>

    init {
        val celestials = staticDatabase.transaction {
            Celestials.selectAll().map {
                Celestial(
                    id = it[Celestials.id],
                    typeId = it[Celestials.typeId],
                    solarSystemId = it[Celestials.solarSystemId],
                    position = Position(
                        x = it[Celestials.x].toDouble(),
                        y = it[Celestials.y].toDouble(),
                        z = it[Celestials.z].toDouble(),
                    ),
                    radius = it[Celestials.radius],
                    name = it[Celestials.name],
                )
            }
        }
        celestialsBySolarSystemId = celestials.groupBy { it.solarSystemId }
    }

    /**
     * @return Closest celestial and distance in meters, or null if none found
     */
    fun getClosestCelestial(solarSystemId: Int, position: Position): ClosestCelestial? {
        val celestials = celestialsBySolarSystemId[solarSystemId] ?: return null
        val (closest, squaredDistance) = celestials
            .map { it to it.position.squaredDistanceTo(position) }
            .minByOrNull { it.second } ?: return null
        val distance = sqrt(squaredDistance) - (closest.radius ?: 0.0)
        return ClosestCelestial(closest, distance)
    }

    private fun Position.squaredDistanceTo(position: Position): Double {
        return (x - position.x).pow(2) + (y - position.y).pow(2) + (z - position.z).pow(2)
    }
}
