package dev.nohus.rift.repositories

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.EsiErrorException
import dev.nohus.rift.network.eveconomy.EveconomyApi
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.settings.persistence.Settings
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.time.delay
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Single
class StructuresRepository(
    private val esiApi: EsiApi,
    private val eveconomyApi: EveconomyApi,
    private val settings: Settings,
    private val solarSystemsRepository: SolarSystemsRepository,
) {

    private val scope = CoroutineScope(SupervisorJob())
    private var updateSettingsJob: Job? = null
    private var unwrittenForbiddenStructures: List<Pair<Int, Long>> = emptyList()
    private val mutex = Mutex()
    private var knownStructures: Map<Long, String>? = null
    private var knownStructuresExpiry: Instant = Instant.now()

    data class Structure(
        val structureId: Long,
        val name: String,
        val ownerId: Int?,
        val typeId: Int?,
        val solarSystemId: Int,
    )

    suspend fun getStructure(
        originator: Originator,
        structureId: Long,
        characterId: Int,
    ): Result<Structure> {
        val forbiddenStructures = settings.forbiddenStructures[characterId] ?: emptySet()
        if (structureId in forbiddenStructures) {
            // This character was forbidden from requesting this structure before. Do not attempt again.
            getKnownStructure(originator, structureId)?.let {
                return Result.Success(it)
            }
            return Result.Failure()
        }

        val result = esiApi.getUniverseStructuresId(originator, structureId, characterId)

        if (result is Result.Failure && result.cause is EsiErrorException && result.cause.code == 403) {
            logger.warn { "${originator}, Structure $structureId was Forbidden from character $characterId" }
            mutex.withLock {
                unwrittenForbiddenStructures += characterId to structureId
                updateSettingsJob?.cancel()
                updateSettingsJob = scope.launch {
                    delay(Duration.ofSeconds(10))
                    mutex.withLock {
                        val newForbidden = unwrittenForbiddenStructures
                            .groupBy({ it.first }, { it.second })
                            .mapValues { (_, values) -> values.toSet() }
                        settings.forbiddenStructures = settings.forbiddenStructures.toMutableMap().apply {
                            newForbidden.forEach { (characterId, structures) ->
                                this[characterId] = getOrDefault(characterId, emptySet()) + structures
                            }
                        }
                        unwrittenForbiddenStructures = emptyList()
                        logger.debug { "Forbidden structures now: ${settings.forbiddenStructures}" }
                    }
                }
            }
            getKnownStructure(originator, structureId)?.let {
                return Result.Success(it)
            }
        }

        return result.map {
            Structure(
                structureId = structureId,
                name = it.name,
                ownerId = it.ownerId,
                typeId = it.typeId,
                solarSystemId = it.solarSystemId
            )
        }
    }

    private suspend fun getKnownStructure(originator: Originator, structureId: Long): Structure? {
        val text = getKnownStructures(originator)[structureId] ?: return null
        val systemName = text.substringBefore(" - ")
        val solarSystemId = solarSystemsRepository.getSystem(systemName)?.id
        if (solarSystemId == null) {
            logger.warn { "System $systemName not found, from known structure $structureId - $text" }
            return null
        }
        return Structure(
            structureId = structureId,
            name = text,
            ownerId = null,
            typeId = null,
            solarSystemId = solarSystemId
        ).also {
            logger.debug { "Replacing Forbidden structure with known structure ${it.name}" }
        }
    }

    private suspend fun getKnownStructures(originator: Originator): Map<Long, String> {
        return mutex.withLock {
            if (Instant.now().isAfter(knownStructuresExpiry)) {
                knownStructures = null
            }
            knownStructures?.let { return it }
            eveconomyApi.getStructures(originator).success?.also {
                knownStructures = it
                knownStructuresExpiry = Instant.now() + Duration.ofDays(1)
            } ?: emptyMap()
        }
    }
}
