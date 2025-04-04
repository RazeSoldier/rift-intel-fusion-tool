package dev.nohus.rift.repositories

import dev.nohus.rift.database.static.Celestials
import dev.nohus.rift.database.static.StaticDatabase
import dev.nohus.rift.repositories.TypesRepository.Type
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.selectAll
import org.koin.core.annotation.Single

@Single
class CelestialsRepository(
    staticDatabase: StaticDatabase,
    typesRepository: TypesRepository,
) {

    data class Celestial(
        val id: Int,
        val type: Type,
        val solarSystemId: Int,
        val position: Position,
        val radius: Double?,
        val name: String,
    )

    data class ClosestCelestial(
        val celestial: Celestial,
        val distance: Double,
    )

    private val scope = CoroutineScope(Job())
    private lateinit var celestialsBySolarSystemId: Map<Int, List<Celestial>>
    private val hasLoaded = CompletableDeferred<Unit>()

    init {
        scope.launch(Dispatchers.IO) {
            val celestials = staticDatabase.transaction {
                Celestials.selectAll().map {
                    Celestial(
                        id = it[Celestials.id],
                        type = typesRepository.getType(it[Celestials.typeId]) ?: error("Missing celestial type: ${it[Celestials.typeId]}"),
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
            hasLoaded.complete(Unit)
        }
    }

    private fun blockUntilLoaded() {
        runBlocking {
            hasLoaded.await()
        }
    }

    /**
     * @return Closest celestial and distance in meters, or null if none found
     */
    fun getClosestCelestial(solarSystemId: Int, position: Position): ClosestCelestial? {
        blockUntilLoaded()
        val celestials = celestialsBySolarSystemId[solarSystemId] ?: return null
        val celestial = celestials.minByOrNull { it.position.squaredDistanceTo(position) } ?: return null
        val warpInPoint = WarpInPoints.getWarpInPoint(celestial)
        val distanceToWarpInPoint = if (warpInPoint != null) {
            position.distanceTo(warpInPoint)
        } else {
            position.distanceTo(celestial.position) - (celestial.radius ?: 0.0)
        }
        return ClosestCelestial(celestial, distanceToWarpInPoint)
    }
}
