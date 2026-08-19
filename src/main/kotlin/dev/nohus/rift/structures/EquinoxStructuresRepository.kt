package dev.nohus.rift.structures

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.CharacterInfo
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.generated.resources.Res
import dev.nohus.rift.generated.resources.magmatic_gas_32px
import dev.nohus.rift.generated.resources.superionic_ice_32px
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.MercenaryDenAnarchyLevel
import dev.nohus.rift.network.esi.models.MercenaryDenDevelopmentLevel
import dev.nohus.rift.network.esi.models.MercenaryDenState
import dev.nohus.rift.network.esi.models.MercenaryDensId
import dev.nohus.rift.network.esi.models.ReinforcementTimer
import dev.nohus.rift.network.esi.models.SkyhookReagent
import dev.nohus.rift.network.esi.models.SkyhookState
import dev.nohus.rift.network.esi.models.SkyhooksId
import dev.nohus.rift.network.esi.models.SovereigntyHubResources
import dev.nohus.rift.network.esi.models.SovereigntyHubUpgradePowerState
import dev.nohus.rift.network.esi.models.SovereigntyHubsId
import dev.nohus.rift.network.esi.models.VulnerabilityWindow
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.repositories.GetSolarSystemChipStateUseCase
import dev.nohus.rift.repositories.PlanetsRepository
import dev.nohus.rift.repositories.PlanetsRepository.Planet
import dev.nohus.rift.repositories.SolarSystemChipLocation
import dev.nohus.rift.repositories.SolarSystemChipState
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository.MapSolarSystem
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.TypesRepository.Type
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository.CorporationDetails
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.sovupgrades.SovereigntyUpgradesTypesRepository
import dev.nohus.rift.sovupgrades.SovereigntyUpgradesTypesRepository.SovereigntyUpgradeType
import dev.nohus.rift.sso.scopes.ScopeGroups
import dev.nohus.rift.structures.PlanetResourcesRepository.PlanetResource
import dev.nohus.rift.structures.EquinoxStructuresRepository.SovereigntyHubWorkforceTransport.Import.ImportSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import org.jetbrains.compose.resources.DrawableResource
import org.koin.core.annotation.Single
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

@Single
class EquinoxStructuresRepository(
    private val esiApi: EsiApi,
    private val localCharactersRepository: LocalCharactersRepository,
    private val typesRepository: TypesRepository,
    private val planetsRepository: PlanetsRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val planetResourcesRepository: PlanetResourcesRepository,
    private val getSolarSystemChipStateUseCase: GetSolarSystemChipStateUseCase,
    private val sovereigntyUpgradesTypesRepository: SovereigntyUpgradesTypesRepository,
    private val characterDetailsRepository: CharacterDetailsRepository,
    private val settings: Settings,
) {

    data class Structures(
        val skyhooks: List<Skyhook> = emptyList(),
        val mercenaryDens: List<MercenaryDen> = emptyList(),
        val sovHubs: List<SovereigntyHub> = emptyList(),
    )

    data class MercenaryDen(
        val id: Long,
        val systemChip: SolarSystemChipState,
        val system: MapSolarSystem,
        val planet: Planet,
        val evolution: MercenaryDenEvolution,
        val infomorphs: Int,
        val reinforcementTimer: ReinforcementTimer?,
        val state: MercenaryDenState,
        val skyhookCorporationId: Int,
        val skyhookCorporation: CorporationDetails?,
        val character: CharacterInfo,
    )

    data class MercenaryDenEvolution(
        val anarchyAmount: Int,
        val anarchyLevel: Int,
        val developmentAmount: Int,
        val developmentLevel: Int,
    )

    data class Skyhook(
        val id: Long,
        val systemChip: SolarSystemChipState,
        val system: MapSolarSystem,
        val planet: Planet,
        val resource: SkyhookResource?,
        val isEnabled: Boolean?,
        val reinforcementTimer: ReinforcementTimer?,
        val state: SkyhookState,
        val theftVulnerability: VulnerabilityWindow?,
        val character: CharacterInfo,
    )

    sealed interface SkyhookResource {
        data class Power(
            val power: Int,
        ) : SkyhookResource

        data class Workforce(
            /**
             * Accounting for loses from Mercenary Dens
             */
            val effectiveWorkforce: Int,
            val workforce: Int,
        ) : SkyhookResource

        data class Reagent(
            val name: String,
            val icon: DrawableResource,
            val typeVolume: Float,
            val amountPerCycle: Int,
            val cyclePeriod: Int,
            val securedStock: Int,
            val securedCapacity: Int,
            val securedStockFullTimestamp: Instant?,
            val unsecuredStock: Int,
            val unsecuredCapacity: Int,
            val unsecuredStockFullTimestamp: Instant?,
        ) : SkyhookResource
    }

    data class SovereigntyHub(
        val id: Long,
        val systemChip: SolarSystemChipState,
        val system: MapSolarSystem,
        val reagentBay: List<SovereigntyHubReagent>,
        val reagentBayLastUpdated: Instant,
        val resources: SovereigntyHubResources,
        val upgrades: List<SovereigntyHubUpgrade>,
        val workforceTransportConfiguration: SovereigntyHubWorkforceTransport,
        val workforceTransportState: SovereigntyHubWorkforceTransport,
        val vulnerabilityWindow: VulnerabilityWindow?,
        val character: CharacterInfo,
    )

    data class SovereigntyHubReagent(
        val type: SovereigntyReagent,
        val name: String,
        val icon: DrawableResource,
        val amount: Int,
        val burningPerHour: Int,
    )

    sealed interface SovereigntyHubWorkforceTransport {
        data class Import(
            val sources: List<ImportSource>,
        ) : SovereigntyHubWorkforceTransport {
            data class ImportSource(
                val amount: Int,
                val solarSystem: MapSolarSystem?,
            )
        }

        data class Export(
            val amount: Int,
            val solarSystem: MapSolarSystem?,
        ) : SovereigntyHubWorkforceTransport

        data object Transit : SovereigntyHubWorkforceTransport

        data object Idle : SovereigntyHubWorkforceTransport

        fun isMatching(configuration: SovereigntyHubWorkforceTransport): Boolean {
            return when (this) {
                is Import -> sources.map { it.solarSystem } == (configuration as? Import)?.sources?.map { it.solarSystem }
                else -> this == configuration
            }
        }
    }

    data class SovereigntyHubUpgrade(
        val type: Type,
        val powerState: SovereigntyHubUpgradePowerState,
        val details: SovereigntyUpgradeType,
    )

    private val _structures = MutableStateFlow(Structures())
    val structures = _structures.asStateFlow()

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
            reloadRequest.filter { it }.collect {
                reloadRequest.value = false
                updateStructures()
            }
        }
    }

    fun setNeedsRealtimeUpdates(isRealtime: Boolean) {
        this.isRealtime = isRealtime
    }

    private suspend fun updateStructures() {
        loadingMutex.withLock {
            _structures.value = load()
        }
    }

    private suspend fun load(): Structures {
        val characters = localCharactersRepository.characters.value
            .filter { it.info != null }
            .filter { ScopeGroups.readYourStructures in it.scopes }
        if (characters.isEmpty()) return Structures()
        val now = Instant.now()

        logger.debug { "Loading structures" }

        val mercenaryDens = characters.flatMap { character ->
            val mercenaryDens = esiApi.getCharactersIdStructuresMercenaryDens(Originator.Structures, character.characterId)
            mercenaryDens.success?.mercenaryDens?.mapNotNull { mercenaryDen ->
                val response = esiApi.getCharactersIdStructuresMercenaryDensId(Originator.Structures, character.characterId, mercenaryDen.id).success
                    ?: return@mapNotNull null
                getMercenaryDen(response, character.info!!)
            } ?: emptyList()
        }.sortedBy { it.planet.id }

        val skyhooks = getStationManagerPerCorporation(characters)
            .flatMap { (character, characterInfo) ->
                val skyhooks = esiApi.getCorporationsIdStructuresSkyhooks(Originator.Structures, character.characterId, characterInfo.corporationId)
                skyhooks.success?.skyhooks?.mapNotNull { skyhook ->
                    val response = esiApi.getCorporationsIdStructuresSkyhooksId(Originator.Structures, character.characterId, characterInfo.corporationId, skyhook.id).success
                        ?: return@mapNotNull null
                    getSkyhook(response, characterInfo, now)
                } ?: emptyList()
            }.sortedBy { it.planet.id }

        val sovHubs = getStationManagerPerCorporation(characters)
            .flatMap { (character, characterInfo) ->
                val sovHubs = esiApi.getCorporationsIdStructuresSovereigntyHubs(Originator.Structures, character.characterId, characterInfo.corporationId)
                sovHubs.success?.sovereigntyHubs?.mapNotNull { sovHub ->
                    val response = esiApi.getCorporationsIdStructuresSovereigntyHubsId(Originator.Structures, character.characterId, characterInfo.corporationId, sovHub.id).success
                        ?: return@mapNotNull null
                    getSovereigntyHub(response, sovHub.solarSystemId, characterInfo)
                } ?: emptyList()
            }

        logger.debug { "Loaded structures: ${mercenaryDens.size} mercenary dens, ${skyhooks.size} skyhooks, ${sovHubs.size} sovereignty hubs" }

        return Structures(
            skyhooks = skyhooks,
            mercenaryDens = mercenaryDens,
            sovHubs = sovHubs,
        )
    }

    private suspend fun getMercenaryDen(
        mercenaryDen: MercenaryDensId,
        characterInfo: CharacterInfo,
    ): MercenaryDen? {
        val planet = planetsRepository.getPlanetById(mercenaryDen.skyhook.planetId) ?: return null
        val system = solarSystemsRepository.getSystem(planet.systemId) ?: return null
        val skyhookCorporation = characterDetailsRepository.getCorporationDetails(Originator.Structures, mercenaryDen.skyhook.corporationId)

        return MercenaryDen(
            id = mercenaryDen.id,
            systemChip = getSolarSystemChipStateUseCase(
                location = SolarSystemChipLocation.SolarSystem(system.id),
                isShowingRegion = true,
            ),
            system = system,
            planet = planet,
            evolution = MercenaryDenEvolution(
                anarchyAmount = mercenaryDen.evolution.anarchy.amount,
                developmentAmount = mercenaryDen.evolution.development.amount,
                anarchyLevel = when (mercenaryDen.evolution.anarchy.level) {
                    MercenaryDenAnarchyLevel.Unspecified -> 0
                    MercenaryDenAnarchyLevel.Level0 -> 0
                    MercenaryDenAnarchyLevel.Level1 -> 1
                    MercenaryDenAnarchyLevel.Level2 -> 2
                    MercenaryDenAnarchyLevel.Level3 -> 3
                    MercenaryDenAnarchyLevel.Level4 -> 4
                },
                developmentLevel = when (mercenaryDen.evolution.development.level) {
                    MercenaryDenDevelopmentLevel.Unspecified -> 0
                    MercenaryDenDevelopmentLevel.Level0 -> 0
                    MercenaryDenDevelopmentLevel.Level1 -> 1
                    MercenaryDenDevelopmentLevel.Level2 -> 2
                    MercenaryDenDevelopmentLevel.Level3 -> 3
                    MercenaryDenDevelopmentLevel.Level4 -> 4
                },
            ),
            infomorphs = mercenaryDen.infomorphs.amount,
            reinforcementTimer = mercenaryDen.reinforcementTimer,
            state = mercenaryDen.state,
            skyhookCorporationId = mercenaryDen.skyhook.corporationId,
            skyhookCorporation = skyhookCorporation,
            character = characterInfo,
        )
    }

    private fun getSkyhook(
        skyhook: SkyhooksId,
        characterInfo: CharacterInfo,
        now: Instant,
    ): Skyhook? {
        val planet = planetsRepository.getPlanetById(skyhook.planetId) ?: return null
        val system = solarSystemsRepository.getSystem(planet.systemId) ?: return null
        val planetResource = planetResourcesRepository.get(planet.id)
        return Skyhook(
            id = skyhook.id,
            systemChip = getSolarSystemChipStateUseCase(
                location = SolarSystemChipLocation.SolarSystem(system.id),
                isShowingRegion = true,
            ),
            system = system,
            planet = planet,
            resource = getSkyhookResource(skyhook, planetResource, now),
            isEnabled = skyhook.isEnabled,
            reinforcementTimer = skyhook.reinforcementTimer,
            state = skyhook.state,
            theftVulnerability = skyhook.theftVulnerability,
            character = characterInfo,
        )
    }

    /**
     * Combines ESI data on current Skyhook production with SDE data on what the planet produces
     */
    private fun getSkyhookResource(
        skyhook: SkyhooksId,
        planetResource: PlanetResource?,
        now: Instant,
    ): SkyhookResource? {
        return when (planetResource) {
            is PlanetResource.Power -> SkyhookResource.Power(planetResource.power)
            is PlanetResource.Workforce -> SkyhookResource.Workforce(
                effectiveWorkforce = skyhook.effectiveWorkforce ?: return null,
                workforce = planetResource.workforce,
            )
            is PlanetResource.Reagent -> {
                val reagent = skyhook.reagents
                    ?.firstOrNull { it.typeId == planetResource.typeId }
                    ?: return null
                val (name, icon) = when (planetResource.typeId) {
                    81144 -> "Superionic Ice" to Res.drawable.superionic_ice_32px
                    81143 -> "Magmatic Gas" to Res.drawable.magmatic_gas_32px
                    else -> return null
                }
                val simulationResult = simulateReagentExtraction(reagent, planetResource, now)
                SkyhookResource.Reagent(
                    name = name,
                    icon = icon,
                    typeVolume = typesRepository.getTypeOrPlaceholder(planetResource.typeId).volume,
                    amountPerCycle = planetResource.amountPerCycle,
                    cyclePeriod = planetResource.cyclePeriod,
                    securedStock = simulationResult.securedStock,
                    securedCapacity = planetResource.securedCapacity,
                    securedStockFullTimestamp = simulationResult.securedStockFullTimestamp,
                    unsecuredStock = simulationResult.unsecuredStock,
                    unsecuredCapacity = planetResource.unsecuredCapacity,
                    unsecuredStockFullTimestamp = simulationResult.unsecuredStockFullTimestamp,
                )
            }
            else -> null
        }
    }

    private data class ReagentSimulationResult(
        val securedStock: Int,
        val unsecuredStock: Int,
        val securedStockFullTimestamp: Instant?,
        val unsecuredStockFullTimestamp: Instant?,
    )

    private fun simulateReagentExtraction(
        reagent: SkyhookReagent,
        planetResource: PlanetResource.Reagent,
        now: Instant,
    ): ReagentSimulationResult {
        val timePassed = Duration.between(reagent.lastCycle, now)
        val cyclesPassed = (timePassed.toSeconds() / planetResource.cyclePeriod).toInt()

        var securedStockNow: Int? = null
        var unsecuredStockNow: Int? = null
        var securedStockFullTimestamp: Instant? = null
        var unsecuredStockFullTimestamp: Instant? = null

        var securedStock = reagent.securedStock
        var unsecuredStock = reagent.unsecuredStock
        var cycle = 0
        do {
            cycle++
            var securedInput = planetResource.amountPerCycle / 2
            var unsecuredInput = planetResource.amountPerCycle - securedInput

            fun addSecuredStock() {
                if (securedInput > 0) {
                    // Add input into secured bay
                    val freeSecuredSpace = planetResource.securedCapacity - securedStock
                    val fittingInput = securedInput.coerceAtMost(freeSecuredSpace)
                    securedStock += fittingInput
                    // Overflow into unsecured input
                    unsecuredInput += securedInput - fittingInput
                    securedInput = 0
                }
            }

            fun addUnsecuredStock() {
                if (unsecuredInput > 0) {
                    // Add input into unsecured bay
                    val freeUnsecuredSpace = planetResource.unsecuredCapacity - unsecuredStock
                    val fittingInput = unsecuredInput.coerceAtMost(freeUnsecuredSpace)
                    unsecuredStock += fittingInput
                    // Overflow into secured input
                    securedInput += unsecuredInput - fittingInput
                    unsecuredInput = 0
                }
            }

            addSecuredStock()
            addUnsecuredStock()
            // Called again to handle new input that might have overflown from unsecured stock not fitting in
            addSecuredStock()

            if (cycle == cyclesPassed) {
                securedStockNow = securedStock
                unsecuredStockNow = unsecuredStock
            }
            if (securedStockFullTimestamp == null && securedStock == planetResource.securedCapacity) {
                securedStockFullTimestamp = reagent.lastCycle + Duration.ofSeconds(cycle * planetResource.cyclePeriod.toLong())
            }
            if (unsecuredStockFullTimestamp == null && unsecuredStock == planetResource.unsecuredCapacity) {
                unsecuredStockFullTimestamp = reagent.lastCycle + Duration.ofSeconds(cycle * planetResource.cyclePeriod.toLong())
            }
        } while (securedStock < planetResource.securedCapacity || unsecuredStock < planetResource.unsecuredCapacity)

        return ReagentSimulationResult(
            securedStock = securedStockNow ?: securedStock,
            unsecuredStock = unsecuredStockNow ?: unsecuredStock,
            securedStockFullTimestamp = securedStockFullTimestamp,
            unsecuredStockFullTimestamp = unsecuredStockFullTimestamp,
        )
    }

    private fun getSovereigntyHub(
        sovereigntyHub: SovereigntyHubsId,
        solarSystemId: Int,
        characterInfo: CharacterInfo,
    ): SovereigntyHub? {
        val system = solarSystemsRepository.getSystem(solarSystemId) ?: return null
        val reagents = listOf(81143, 81144).map { typeId ->
            val reagent = sovereigntyHub.reagentBay.reagents.firstOrNull { it.typeId == typeId }
            val (name, icon, type) = when (typeId) {
                81144 -> Triple("Superionic Ice", Res.drawable.superionic_ice_32px, SovereigntyReagent.SuperionicIce)
                81143 -> Triple("Magmatic Gas", Res.drawable.magmatic_gas_32px, SovereigntyReagent.MagmaticGas)
                else -> return null
            }
            SovereigntyHubReagent(
                type = type,
                name = name,
                icon = icon,
                amount = reagent?.amount ?: 0,
                burningPerHour = reagent?.burningPerHour ?: 0,
            )
        }

        val workforceTransportConfiguration = sovereigntyHub.workforceTransport.configuration.let {
            when {
                it.transit == true -> SovereigntyHubWorkforceTransport.Transit
                it.import != null -> SovereigntyHubWorkforceTransport.Import(
                    sources = it.import.sources.sortedBy { it.solarSystemId }.map {
                        ImportSource(
                            amount = 0, // Import configuration has no amount, the amount is set in the exporting Sovereignty Hub
                            solarSystem = solarSystemsRepository.getSystem(it.solarSystemId),
                        )
                    },
                )
                it.export != null -> SovereigntyHubWorkforceTransport.Export(
                    amount = it.export.amount,
                    solarSystem = solarSystemsRepository.getSystem(it.export.solarSystemId),
                )
                else -> SovereigntyHubWorkforceTransport.Idle
            }
        }
        val workforceTransportState = sovereigntyHub.workforceTransport.state.let {
            when {
                it.transit == true -> SovereigntyHubWorkforceTransport.Transit
                it.import != null -> SovereigntyHubWorkforceTransport.Import(
                    sources = it.import.sources.sortedBy { it.solarSystemId }.map {
                        ImportSource(
                            amount = it.amount,
                            solarSystem = solarSystemsRepository.getSystem(it.solarSystemId),
                        )
                    },
                )
                it.export != null -> SovereigntyHubWorkforceTransport.Export(
                    amount = it.export.amount,
                    solarSystem = solarSystemsRepository.getSystem(it.export.solarSystemId),
                )
                else -> SovereigntyHubWorkforceTransport.Idle
            }
        }
        val upgrades = sovereigntyHub.upgrades.mapNotNull { upgrade ->
            SovereigntyHubUpgrade(
                type = typesRepository.getTypeOrPlaceholder(upgrade.typeId),
                powerState = upgrade.powerState,
                details = sovereigntyUpgradesTypesRepository.get(upgrade.typeId) ?: return@mapNotNull null,
            )
        }

        return SovereigntyHub(
            id = sovereigntyHub.id,
            systemChip = getSolarSystemChipStateUseCase(
                location = SolarSystemChipLocation.SolarSystem(system.id),
                isShowingRegion = true,
            ),
            system = system,
            reagentBay = reagents,
            reagentBayLastUpdated = sovereigntyHub.reagentBay.lastUpdated,
            resources = sovereigntyHub.resources,
            upgrades = upgrades,
            workforceTransportConfiguration = workforceTransportConfiguration,
            workforceTransportState = workforceTransportState,
            vulnerabilityWindow = sovereigntyHub.vulnerabilityWindow,
            character = characterInfo,
        )
    }

    private fun getStationManagerPerCorporation(characters: List<LocalCharacter>): List<Pair<LocalCharacter, CharacterInfo>> {
        return characters
            .mapNotNull { it to (it.info ?: return@mapNotNull null) }
            .filter {
                if (settings.isEquinoxMockingEnabled) true else it.second.corporationRoles.contains("Station_Manager")
            }
            .groupBy { it.second.corporationId }
            .mapValues { it.value.first() }
            .map { it.value.first to it.value.second }
    }
}
