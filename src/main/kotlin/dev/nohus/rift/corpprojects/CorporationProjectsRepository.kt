package dev.nohus.rift.corpprojects

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.corpprojects.GetProjectContributionAttributesUseCase.ProjectContributionAttribute
import dev.nohus.rift.corpprojects.GetProjectContributionAttributesUseCase.ProjectContributionAttributeType
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.Result.Failure
import dev.nohus.rift.network.Result.Success
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.CorporationProject
import dev.nohus.rift.network.esi.models.CorporationProjectCareer
import dev.nohus.rift.network.esi.models.CorporationProjectState
import dev.nohus.rift.network.esi.models.CorporationProjectsQueryState
import dev.nohus.rift.network.esi.pagination.fetchCursorPaginated
import dev.nohus.rift.repositories.GetSolarSystemChipStateUseCase
import dev.nohus.rift.repositories.IdRanges
import dev.nohus.rift.repositories.SolarSystemChipLocation
import dev.nohus.rift.repositories.SolarSystemChipState
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.sso.scopes.ScopeGroups
import dev.nohus.rift.utils.mapAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes

@Single
class CorporationProjectsRepository(
    private val esiApi: EsiApi,
    private val localCharactersRepository: LocalCharactersRepository,
    private val characterDetailsRepository: CharacterDetailsRepository,
    private val mapper: CorporationProjectsMapper,
    private val getSolarSystemChipStateUseCase: GetSolarSystemChipStateUseCase,
    private val getProjectContributionAttributesUseCase: GetProjectContributionAttributesUseCase,
) {

    data class Projects(
        val corporationProjects: List<CorporationProjects> = emptyList(),
        val loading: LoadingState = LoadingState(),
    )

    data class CorporationProjects(
        val corporation: Corporation,
        val projects: List<Project> = emptyList(),
        val deletedProjects: Set<String> = emptySet(),
        val failedProjects: List<Exception?> = emptyList(),
        val failureCause: Exception? = null,
    )

    data class LoadingState(
        val isLoading: Boolean = false,
        val corporations: List<LoadingCorporation> = emptyList(),
    )

    data class LoadingCorporation(
        val corporation: Corporation,
        val projectsCount: Int? = null,
        val loadedProjectsCount: Int = 0,
    )

    private val _projects = MutableStateFlow(Projects())
    val projects = _projects.asStateFlow()

    private val reloadFlow = MutableSharedFlow<Unit>()
    private val loadingMutex = Mutex()
    private var isRealtime = false
    private val afterCursors = mutableMapOf<Int, String?>() // Corporation ID -> cursor

    @OptIn(FlowPreview::class)
    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                delay(1.minutes)
                if (isRealtime) updateProjects()
            }
        }
        launch {
            while (true) {
                delay(15.minutes)
                updateProjects()
            }
        }
        launch {
            localCharactersRepository.characters.debounce(500).collectLatest {
                updateProjects()
            }
        }
        launch {
            reloadFlow.collect {
                updateProjects()
            }
        }
    }

    suspend fun reload() {
        if (!loadingMutex.isLocked) reloadFlow.emit(Unit)
    }

    fun setNeedsRealtimeUpdates(isRealtime: Boolean) {
        this.isRealtime = isRealtime
    }

    private fun updateLoading(update: LoadingState.() -> LoadingState) {
        _projects.update { it.copy(loading = it.loading.update()) }
    }

    private suspend fun updateProjects() {
        loadingMutex.withLock {
            updateLoading { LoadingState(isLoading = true) }
            val existingProjects = _projects.value.corporationProjects
            val updatedProjects = getAllCorporationProjects()
            _projects.update {
                it.copy(
                    corporationProjects = mergeUpdatedCorporationProjects(existingProjects, updatedProjects),
                )
            }
            updateLoading { LoadingState(isLoading = false) }
        }
    }

    /**
     * Merges updated projects into existing projects
     */
    private fun mergeUpdatedCorporationProjects(
        existing: List<CorporationProjects>,
        updated: List<CorporationProjects>,
    ): List<CorporationProjects> {
        val existingByCorporation = existing.associateBy { it.corporation }
        val updatedByCorporation = updated.associateBy { it.corporation }
        val corporations = existingByCorporation.keys + updatedByCorporation.keys
        return corporations.mapNotNull { corporation ->
            val existing = existingByCorporation[corporation]
            val updated = updatedByCorporation[corporation]
            if (existing != null && updated != null) {
                val existingProjects = existing.projects.associateBy { it.id } - updated.deletedProjects
                val updatedProjects = updated.projects.associateBy { it.id }
                val mergedProjects = existingProjects + updatedProjects
                CorporationProjects(
                    corporation = corporation,
                    projects = mergedProjects.values.toList(),
                    failedProjects = updated.failedProjects,
                    failureCause = updated.failureCause,
                )
            } else {
                existing ?: updated
            }
        }
    }

    /**
     * Returns a map of all corporations from local characters and the characters that belong to them,
     * only considering characters that have the read projects scope
     */
    private fun getCorporations(): Map<Corporation, List<LocalCharacter>> {
        return localCharactersRepository.characters.value
            .filter { ScopeGroups.readProjects in it.scopes }
            .mapNotNull { character ->
                val corporation = character.info.success?.let {
                    Corporation(it.corporationId, it.corporationName)
                } ?: return@mapNotNull null
                corporation to character
            }
            .filterNot { IdRanges.isNpcCorporation(it.first.id) }
            .groupBy({ it.first }, { it.second })
    }

    private suspend fun getAllCorporationProjects(): List<CorporationProjects> {
        return getCorporations()
            .entries
            .also {
                updateLoading { copy(corporations = it.map { LoadingCorporation(it.key) }) }
            }
            .mapAsync { (corporation, characters) ->
                val after = afterCursors[corporation.id]
                getCorporationProjects(corporation, characters, after)
            }
    }

    /**
     * Returns all projects from the given corporation, updated after the given cursor, or all if null
     */
    private suspend fun getCorporationProjects(
        corporation: Corporation,
        characters: List<LocalCharacter>,
        after: String?,
    ): CorporationProjects = coroutineScope {
        val characterIds = characters.map { it.characterId }
        val projectManagersDeferred = async {
            characterIds.mapAsync { characterId ->
                characterId to esiApi.getCharactersIdRoles(characterId).map { "Project_Manager" in it.roles }
            }.filter { it.second.success == true }.map { it.first }
        }

        var deletedProjects: Set<String> = emptySet()
        val projects = fetchCursorPaginated(after) { before, after ->
            esiApi.getCorporationsIdProjects(characterIds.first(), corporation.id, before, after, state = CorporationProjectsQueryState.All)
        }.map { (projects, newAfter) ->
            if (after == null) {
                updateLoading { copy(corporations = corporations.map { if (it.corporation == corporation) it.copy(projectsCount = projects.size) else it }) }
            }
            deletedProjects = projects.filter { it.state == CorporationProjectState.Deleted }.map { it.id }.toSet()
            projects
                .filter { it.state != CorporationProjectState.Deleted }
                .map { project ->
                    async {
                        getProject(
                            corporation = corporation,
                            characters = characters,
                            project = project,
                            projectManagersDeferred = projectManagersDeferred,
                        ).also {
                            if (after == null) {
                                updateLoading { copy(corporations = corporations.map { if (it.corporation == corporation) it.copy(loadedProjectsCount = it.loadedProjectsCount + 1) else it }) }
                            }
                        }
                    }
                } to newAfter
        }
            .map { (projects, newAfter) -> projects.awaitAll() to newAfter }
            .onFailure { afterCursors -= corporation.id }
            .onSuccess { (projects, newAfter) ->
                afterCursors[corporation.id] = newAfter.takeIf { projects.all { it.isSuccess } }
            }
            .map { it.first }

        when (projects) {
            is Success -> CorporationProjects(
                corporation = corporation,
                projects = projects.data.filterIsInstance<Success<Project>>().map { it.data },
                deletedProjects = deletedProjects,
                failedProjects = projects.data.filterIsInstance<Failure>().map { it.cause },
            )
            is Failure -> CorporationProjects(
                corporation = corporation,
                failureCause = projects.cause,
            )
        }
    }

    private suspend fun CoroutineScope.getProject(
        corporation: Corporation,
        characters: List<LocalCharacter>,
        project: CorporationProject,
        projectManagersDeferred: Deferred<List<Int>>,
    ): Result<Project> {
        val characterIds = characters.map { it.characterId }
        val detailsDeferred = async {
            esiApi.getCorporationsIdProjectsId(characterIds.first(), corporation.id, project.id)
        }
        val contributionsDeferred = characters.map { character ->
            async {
                val contributionResult = esiApi.getCorporationsIdProjectsIdContribution(character.characterId, corporation.id, project.id)
                    .map { it.contributed }
                Contribution(
                    characterId = character.characterId,
                    characterName = character.info.success?.name ?: "?",
                    contribution = contributionResult,
                )
            }
        }
        val contributorsDeferred: Deferred<Contributors> = async {
            projectManagersDeferred.await().firstOrNull()?.let { projectManager ->
                fetchCursorPaginated(null) { before, after ->
                    esiApi.getCorporationsIdProjectsIdContributors(projectManager, corporation.id, project.id, before, after)
                }.map { (contributors, newAfter) ->
                    val list = contributors.mapAsync {
                        Contributor(
                            characterId = it.id.toInt(),
                            details = characterDetailsRepository.getCharacterDetails(it.id.toInt()),
                            contributed = it.contributed,
                        )
                    }
                    if (list.isNotEmpty()) Contributors.Available(list) else Contributors.Empty
                }.let {
                    when (it) {
                        is Failure -> Contributors.Error(it.cause?.message ?: "Unknown error")
                        is Success -> it.data
                    }
                }
            } ?: Contributors.NoAccess
        }
        val details = when (val details = detailsDeferred.await()) {
            is Success -> details.data
            is Failure -> return details
        }
        val creatorDeferred = async {
            characterDetailsRepository.getCharacterDetails(details.creator.id)
        }
        val configuration = mapper.toModel(details.configuration)
        val contributionAttributesDeferred = async {
            getProjectContributionAttributesUseCase(configuration, characterIds.first())
        }
        val matchingFilters = getMatchingFilters(details.details.career, configuration)
        val contributionAttributes = contributionAttributesDeferred.await()
        return mapper.toModel(
            corporation = corporation,
            project = project,
            details = details,
            configuration = configuration,
            contributionAttributes = contributionAttributes,
            solarSystemChipState = getSolarSystemChipState(contributionAttributes),
            matchingFilters = matchingFilters,
            creator = creatorDeferred.await(),
            contributions = contributionsDeferred.awaitAll(),
            contributors = contributorsDeferred.await(),
            eligibleCharacters = characters,
        ).let { Success(it) }
    }

    private fun getSolarSystemChipState(
        contributionAttributes: List<ProjectContributionAttributeType>,
    ): SolarSystemChipState? {
        val solarSystemChipLocations = contributionAttributes.flatMap { it.values }.mapNotNull { value ->
            when (value) {
                is ProjectContributionAttribute.SolarSystem -> SolarSystemChipLocation.SolarSystem(value.solarSystem.id)
                is ProjectContributionAttribute.Constellation -> SolarSystemChipLocation.Constellation(value.constellation.id)
                is ProjectContributionAttribute.Region -> SolarSystemChipLocation.Region(value.region.id)
                is ProjectContributionAttribute.Station -> value.solarSystem?.id?.let { SolarSystemChipLocation.SolarSystem(it) }
                is ProjectContributionAttribute.Structure -> value.solarSystem?.id?.let { SolarSystemChipLocation.SolarSystem(it) }
                else -> null
            }
        }
        if (solarSystemChipLocations.isEmpty()) return null
        return getSolarSystemChipStateUseCase(solarSystemChipLocations)
    }

    private fun getMatchingFilters(career: CorporationProjectCareer, configuration: ProjectConfiguration?): List<ProjectCategoryFilter> {
        return buildList {
            career.let {
                when (it) {
                    CorporationProjectCareer.Explorer -> ProjectCategoryFilter.Explorer
                    CorporationProjectCareer.Industrialist -> ProjectCategoryFilter.Industrialist
                    CorporationProjectCareer.Enforcer -> ProjectCategoryFilter.Enforcer
                    CorporationProjectCareer.SoldierOfFortune -> ProjectCategoryFilter.SoldierOfFortune
                    else -> null
                }
            }?.let { add(it) }

            when (configuration) {
                is ProjectConfiguration.CaptureFwComplex -> listOf(ProjectCategoryFilter.FactionalWarfare, ProjectCategoryFilter.Combat)
                is ProjectConfiguration.DamageShip -> listOf(ProjectCategoryFilter.Combat)
                is ProjectConfiguration.DefendFwComplex -> listOf(ProjectCategoryFilter.FactionalWarfare, ProjectCategoryFilter.Combat)
                is ProjectConfiguration.DeliverItem -> listOf(ProjectCategoryFilter.Hauling)
                is ProjectConfiguration.DestroyNpc -> listOf(ProjectCategoryFilter.Combat)
                is ProjectConfiguration.DestroyShip -> listOf(ProjectCategoryFilter.Combat)
                is ProjectConfiguration.EarnLoyaltyPoint -> listOf()
                is ProjectConfiguration.LostShip -> listOf(ProjectCategoryFilter.Combat, ProjectCategoryFilter.Fleet, ProjectCategoryFilter.Logistics)
                ProjectConfiguration.Manual -> listOf()
                is ProjectConfiguration.ManufactureItem -> listOf(ProjectCategoryFilter.Manufacturing)
                is ProjectConfiguration.MineMaterial -> listOf(ProjectCategoryFilter.Mining)
                is ProjectConfiguration.RemoteBoostShield -> listOf(ProjectCategoryFilter.Combat, ProjectCategoryFilter.Fleet, ProjectCategoryFilter.Logistics)
                is ProjectConfiguration.RemoteRepairArmor -> listOf(ProjectCategoryFilter.Combat, ProjectCategoryFilter.Fleet, ProjectCategoryFilter.Logistics)
                is ProjectConfiguration.SalvageWreck -> listOf()
                is ProjectConfiguration.ScanSignature -> listOf(ProjectCategoryFilter.CosmicSignatures)
                is ProjectConfiguration.ShipInsurance -> listOf(ProjectCategoryFilter.Combat, ProjectCategoryFilter.Fleet, ProjectCategoryFilter.Logistics)
                is ProjectConfiguration.Unknown -> listOf()
                null -> listOf()
            }.let { addAll(it) }
        }
    }
}
