package dev.nohus.rift.opportunities

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.MercenaryTacticalOperationState
import dev.nohus.rift.network.esi.models.MercenaryTacticalOperationsId
import dev.nohus.rift.network.esi.models.OpportunityCareer
import dev.nohus.rift.network.esi.models.OpportunityState
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.opportunities.MercenaryTacticalOperationsTypesRepository.MercenaryTacticalOperationType
import dev.nohus.rift.opportunities.OpportunitiesUtils.getMatchingFilters
import dev.nohus.rift.repositories.GetSolarSystemChipStateUseCase
import dev.nohus.rift.repositories.SolarSystemChipLocation
import dev.nohus.rift.sso.scopes.ScopeGroups
import dev.nohus.rift.structures.StructuresRepository
import dev.nohus.rift.structures.StructuresRepository.MercenaryDen
import dev.nohus.rift.utils.mapAsync
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import org.koin.core.annotation.Single
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

@Single
class MercenaryTacticalOperationsRepository(
    private val esiApi: EsiApi,
    private val localCharactersRepository: LocalCharactersRepository,
    private val structuresRepository: StructuresRepository,
    private val mercenaryTacticalOperationsTypesRepository: MercenaryTacticalOperationsTypesRepository,
    private val mapper: OpportunitiesMapper,
    private val getSolarSystemChipStateUseCase: GetSolarSystemChipStateUseCase,
    private val getOpportunityContributionAttributesUseCase: GetOpportunityContributionAttributesUseCase,
) {

    data class Operations(
        val operations: List<MercenaryTacticalOperation> = emptyList(),
        val opportunities: List<Opportunity> = emptyList(),
        val isLoading: Boolean = false,
    )

    data class MercenaryTacticalOperation(
        val id: String,
        val mercenaryDen: MercenaryDen?,
        val type: MercenaryTacticalOperationType?,
        val expires: Instant,
        val state: MercenaryTacticalOperationState,
        val character: LocalCharacter,
    )

    private val _operations = MutableStateFlow(Operations())
    val operations = _operations.asStateFlow()

    private val reloadRequest = MutableStateFlow(false)
    private val loadingMutex = Mutex()
    private var isRealtime = false

    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                delay(1.minutes)
                if (isRealtime) {
                    reloadRequest.value = false
                    yield()
                    reloadRequest.value = true
                }
            }
        }
        launch {
            while (true) {
                delay(15.minutes)
                reloadRequest.value = true
            }
        }
        launch {
            localCharactersRepository.characters.debounce(1000).collectLatest {
                reloadRequest.value = true
            }
        }
        launch {
            structuresRepository.structures.map { it.mercenaryDens }.collectLatest {
                reloadRequest.value = true
            }
        }
        launch {
            reloadRequest.filter { it }.collect {
                reloadRequest.value = false
                updateOperations()
            }
        }
    }

    fun reload() {
        if (!loadingMutex.isLocked) reloadRequest.value = true
    }

    fun setNeedsRealtimeUpdates(isRealtime: Boolean) {
        this.isRealtime = isRealtime
    }

    private suspend fun updateOperations() {
        loadingMutex.withLock {
            _operations.update { it.copy(isLoading = true) }

            val characters = localCharactersRepository.characters.value
                .filter { it.info != null }
                .filter { ScopeGroups.readYourStructures in it.scopes }
            val mercenaryDensById = structuresRepository.structures.value.mercenaryDens.associateBy { it.id }
            val operations = characters.flatMap { character ->
                val mtos = esiApi.getCharactersIdMercenaryTacticalOperations(Originator.Structures, character.characterId)
                mtos.success?.operations?.mapNotNull { mto ->
                    val response = esiApi.getCharactersIdMercenaryTacticalOperationsId(Originator.Structures, character.characterId, mto.id).success
                        ?: return@mapNotNull null
                    val mercenaryDen = mercenaryDensById[response.mercenaryDenId]
                    getMercenaryTacticalOperation(response, mercenaryDen, character)
                } ?: emptyList()
            }

            val opportunities = operations.mapAsync { operation ->
                val solarSystemChipState = operation.mercenaryDen?.system?.id?.let { solarSystemId ->
                    getSolarSystemChipStateUseCase(SolarSystemChipLocation.SolarSystem(solarSystemId))
                }
                val status = when (operation.state) {
                    MercenaryTacticalOperationState.Unspecified -> "Unspecified"
                    MercenaryTacticalOperationState.Available -> "Ready to Start"
                    MercenaryTacticalOperationState.Started -> "In progress"
                    MercenaryTacticalOperationState.Completed -> "Completed"
                    MercenaryTacticalOperationState.Expired -> "Expired"
                    MercenaryTacticalOperationState.Removed -> "Removed"
                }
                val configuration = operation.type?.let { type ->
                    mapper.toModel(type, operation.mercenaryDen?.system, status)
                } ?: OpportunityConfiguration.Unknown("Mercenary Tactical Operation")
                val contributionAttributesDeferred = async {
                    getOpportunityContributionAttributesUseCase(Originator.Structures, configuration, operation.character.characterId)
                }
                val matchingFilters = getMatchingFilters(
                    baseType = OpportunityCategoryFilter.MercenaryTacticalOperations,
                    career = OpportunityCareer.Enforcer,
                    configuration = configuration,
                )
                val contributionAttributes = contributionAttributesDeferred.await()
                mapper.toModel(
                    debugDetails = operation.toString(),
                    operation = operation,
                    configuration = configuration,
                    contributionAttributes = contributionAttributes,
                    solarSystemChipState = solarSystemChipState,
                    matchingFilters = matchingFilters,
                )
            }

            _operations.update {
                it.copy(
                    operations = operations,
                    opportunities = opportunities,
                    isLoading = false,
                )
            }
        }
    }

    private fun getMercenaryTacticalOperation(
        mto: MercenaryTacticalOperationsId,
        mercenaryDen: MercenaryDen?,
        character: LocalCharacter,
    ): MercenaryTacticalOperation {
        val operationType = mercenaryTacticalOperationsTypesRepository.getOperation(mto.dungeonTypeId)
        return MercenaryTacticalOperation(
            id = mto.id,
            mercenaryDen = mercenaryDen,
            type = operationType,
            expires = mto.expires,
            state = mto.state,
            character = character,
        )
    }
}
