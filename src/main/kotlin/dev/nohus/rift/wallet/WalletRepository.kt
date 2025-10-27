package dev.nohus.rift.wallet

import dev.nohus.rift.characters.repositories.LocalCharactersRepository
import dev.nohus.rift.characters.repositories.LocalCharactersRepository.LocalCharacter
import dev.nohus.rift.contacts.ContactsRepository
import dev.nohus.rift.location.LocationRepository
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.EsiApi
import dev.nohus.rift.network.esi.models.ContextIdType
import dev.nohus.rift.network.esi.models.UniverseNamesCategory
import dev.nohus.rift.network.esi.models.WalletJournalEntry
import dev.nohus.rift.network.esi.models.WalletTransaction
import dev.nohus.rift.network.esi.pagination.fetchOffsetIdPaginated
import dev.nohus.rift.network.esi.pagination.fetchPagePaginated
import dev.nohus.rift.repositories.CelestialsRepository
import dev.nohus.rift.repositories.FactionNames
import dev.nohus.rift.repositories.IdRanges
import dev.nohus.rift.repositories.NamesRepository
import dev.nohus.rift.repositories.PlanetsRepository
import dev.nohus.rift.repositories.SolarSystemsRepository
import dev.nohus.rift.repositories.TypesRepository
import dev.nohus.rift.repositories.character.CharacterDetailsRepository
import dev.nohus.rift.sso.scopes.ScopeGroups
import dev.nohus.rift.utils.mapAsync
import dorkbox.util.Sys
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

@Single
class WalletRepository(
    private val localCharactersRepository: LocalCharactersRepository,
    private val locationRepository: LocationRepository,
    private val characterDetailsRepository: CharacterDetailsRepository,
    private val solarSystemsRepository: SolarSystemsRepository,
    private val typesRepository: TypesRepository,
    private val contactsRepository: ContactsRepository,
    private val esiApi: EsiApi,
    private val walletLocalRepository: WalletLocalRepository,
    private val namesRepository: NamesRepository,
    private val celestialsRepository: CelestialsRepository,
    private val walletDivisionsRepository: WalletDivisionsRepository,
) {

    data class State(
        val loadedState: Result<LoadedState>? = null,
        val isLoading: Boolean = false,
    )

    data class LoadedState(
        val journal: List<WalletJournalItem>,
        val balances: List<WalletBalance>,
        val characters: List<Character>,
        val corporations: List<Corporation>,
    )

    sealed class WalletBalance(open val balance: Double) {
        data class Character(
            val characterId: Int,
            override val balance: Double,
        ) : WalletBalance(balance)
        data class Corporation(
            val corporationId: Int,
            val divisionId: Int,
            override val balance: Double,
        ) : WalletBalance(balance)
    }

    data class Character(
        val id: Int,
        val name: String,
    )

    data class Corporation(
        val id: Int,
        val name: String,
    )

    private val _state = MutableStateFlow<State>(State())
    val state = _state.asStateFlow()

    private val reloadFlow = MutableSharedFlow<Boolean>()
    private val loadingMutex = Mutex()
    private var isRealtime = false

    @OptIn(FlowPreview::class)
    suspend fun start() = coroutineScope {
        launch {
            while (true) {
                delay(1.minutes)
                if (isRealtime) reloadFlow.emit(true)
            }
        }
        launch {
            while (true) {
                delay(15.minutes)
                reloadFlow.emit(true)
            }
        }
        launch {
            localCharactersRepository.characters.debounce(500).collectLatest { characters ->
                // Wait for contacts to load before loading wallets, unless they take too long
                withTimeoutOrNull(10_000) {
                    contactsRepository.finishedLoading.filter { it }.first()
                }
                reloadFlow.emit(false)
                reloadFlow.emit(true)
            }
        }
        launch {
            reloadFlow.collect {
                load(isLoadingFromEsi = it)
            }
        }
    }

    suspend fun reload() {
        if (!loadingMutex.isLocked) reloadFlow.emit(true)
    }

    fun setNeedsRealtimeUpdates(isRealtime: Boolean) {
        this.isRealtime = isRealtime
    }

    private suspend fun load(
        isLoadingFromEsi: Boolean,
    ) = withContext(Dispatchers.IO) {
        loadingMutex.withLock {
            _state.update { it.copy(isLoading = true) }
            val localCharacters = localCharactersRepository.characters.value
            if (localCharacters.isEmpty()) return@withContext

            val charactersWithRolesDeferred = async {
                localCharacters.filter {
                    ScopeGroups.readCorporationWallet in it.scopes
                }.mapAsync { character ->
                    character to esiApi.getCharactersIdRoles(character.characterId)
                }.mapNotNull { it.first to (it.second.success?.roles ?: return@mapNotNull null) }
            }
            val accountantsDeferred = async {
                charactersWithRolesDeferred.await().map { (character, roles) ->
                    character.characterId to ("Accountant" in roles || "Junior_Accountant" in roles)
                }.filter { it.second }.map { it.first }
            }
            val directorsDeferred = async {
                charactersWithRolesDeferred.await().map { (character, roles) ->
                    character to ("Director" in roles)
                }.filter { it.second }.map { it.first }
            }
            val divisionNamesJob = launch {
                if (isLoadingFromEsi) {
                    walletDivisionsRepository.load(directorsDeferred.await())
                }
            }

            val accountants = accountantsDeferred.await()
            val corporationsIdsToAccountantIds = localCharacters
                .filter { it.characterId in accountants }
                .mapNotNull { character ->
                    val corporationId = character.info.success?.corporationId ?: return@mapNotNull null
                    corporationId to character.characterId
                }
                .groupBy { it.first }
                .mapValues { it.value.map { it.second }.first() }

            val charactersToLoad = localCharacters
                .filter { ScopeGroups.readWallet in it.scopes }

            val walletBalances = async { getWalletBalances(charactersToLoad, corporationsIdsToAccountantIds) }

            if (isLoadingFromEsi) {
                updateDatabaseFromEsi(charactersToLoad, corporationsIdsToAccountantIds)
            }

            val journalEntriesPerWallet = walletLocalRepository.loadJournalEntries()
            val transactions = walletLocalRepository.loadTransactions()
            val items = journalEntriesPerWallet.mapNotNull { (wallet, journalEntries) ->
                async {
                    val characterId = when (wallet) {
                        is Wallet.Character -> wallet.characterId
                        is Wallet.Corporation -> corporationsIdsToAccountantIds[wallet.corporationId] ?: return@async emptyList()
                    }
                    buildItems(wallet, characterId, journalEntries, transactions)
                }
            }.awaitAll().flatten()

            val characters = charactersToLoad.map {
                Character(it.characterId, it.info.success?.name ?: it.characterId.toString())
            }
            val corporations = corporationsIdsToAccountantIds.map { (corporationId, accountantId) ->
                val name = localCharacters
                    .firstOrNull { it.characterId == accountantId }?.info?.success?.corporationName
                    ?: corporationId.toString()
                Corporation(corporationId, name)
            }

            divisionNamesJob.join()
            _state.update {
                it.copy(
                    loadedState = Result.Success(
                        LoadedState(
                            journal = items,
                            balances = walletBalances.await(),
                            characters = characters,
                            corporations = corporations,
                        ),
                    ),
                    isLoading = false,
                )
            }
        }
    }

    private suspend fun getWalletBalances(
        charactersToLoad: List<LocalCharacter>,
        corporationsIdsToAccountantIds: Map<Int, Int>,
    ): List<WalletBalance> {
        return coroutineScope {
            val characterWalletsDeferred = charactersToLoad.map { character ->
                async {
                    esiApi.getCharacterIdWallet(character.characterId).map {
                        WalletBalance.Character(character.characterId, it)
                    }.success
                }
            }
            val corporationWalletsDeferred = corporationsIdsToAccountantIds.map { (corporationId, accountantId) ->
                async {
                    esiApi.getCorporationsCorporationIdWallet(accountantId, corporationId).map { wallets ->
                        wallets.map { wallet ->
                            WalletBalance.Corporation(corporationId, wallet.divisionId, wallet.balance)
                        }
                    }.success
                }
            }
            val characterWallets = characterWalletsDeferred.awaitAll().filterNotNull()
            val corporationWallets = corporationWalletsDeferred.awaitAll().filterNotNull().flatten()
            characterWallets + corporationWallets
        }
    }

    /**
     * Gets new journal entries and transactions from ESI and saves them to the database
     */
    private suspend fun updateDatabaseFromEsi(
        charactersToLoad: List<LocalCharacter>,
        corporationsIdsToAccountantIds: Map<Int, Int>,
    ) {
        coroutineScope {
            charactersToLoad.forEach { character ->
                val wallet = Wallet.Character(character.characterId)
                launch {
                    val characterId = character.characterId
                    val deferredJournal = async {
                        fetchPagePaginated {
                            esiApi.getCharactersIdWalletJournal(characterId, it)
                        }
                    }
                    val deferredTransactions = async {
                        fetchOffsetIdPaginated {
                            esiApi.getCharactersIdWalletTransactions(characterId, it)
                        }
                    }
                    val journalEntries = deferredJournal.await()
                    val transactions = deferredTransactions.await()

                    if (transactions is Result.Success && journalEntries is Result.Success) {
                        walletLocalRepository.save(wallet, journalEntries.data)
                        walletLocalRepository.save(transactions.data)
                    } else {
                        if (journalEntries is Result.Failure) {
                            logger.error { "Failed to fetch journal entries for character $characterId: ${journalEntries.cause}" }
                        }
                        if (transactions is Result.Failure) {
                            logger.error { "Failed to fetch transactions for character $characterId: ${transactions.cause}" }
                        }
                    }
                }
            }

            corporationsIdsToAccountantIds.forEach { (corporationId, accountantId) ->
                launch {
                    (1..7).forEach { divisionId ->
                        val wallet = Wallet.Corporation(corporationId, divisionId)
                        launch {
                            val deferredJournal = async {
                                fetchPagePaginated {
                                    esiApi.getCorporationsCorporationIdWalletsDivisionJournal(accountantId, corporationId, divisionId, it)
                                }
                            }
                            val deferredTransactions = async {
                                fetchOffsetIdPaginated {
                                    esiApi.getCorporationsCorporationIdWalletsDivisionTransactions(accountantId, corporationId, divisionId, it)
                                }
                            }
                            val journalEntries = deferredJournal.await()
                            val transactions = deferredTransactions.await()

                            if (transactions is Result.Success && journalEntries is Result.Success) {
                                walletLocalRepository.save(wallet, journalEntries.data)
                                walletLocalRepository.save(transactions.data)
                            } else {
                                if (journalEntries is Result.Failure) {
                                    logger.error { "Failed to fetch journal entries for corporation $corporationId division $divisionId: ${journalEntries.cause}" }
                                }
                                if (transactions is Result.Failure) {
                                    logger.error { "Failed to fetch transactions for corporation $corporationId division $divisionId: ${transactions.cause}" }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Builds a list of [WalletJournalItem]s from the given journal entries and transactions.
     * characterId is for fetching structure details
     */
    private suspend fun buildItems(
        wallet: Wallet,
        characterId: Int,
        journalEntries: List<WalletJournalEntry>,
        transactions: List<WalletTransaction>,
    ): List<WalletJournalItem> {
        val entries = fixJournalEntries(journalEntries)
        val details = loadTypeDetails(characterId, entries, transactions)
        val transactionsByJournalId = transactions.associateBy { it.journalRefId }
        val items = entries.map { entry ->
            val transaction = transactionsByJournalId[entry.id]?.let {
                WalletTransactionItem(
                    client = details[it.clientId],
                    date = it.date,
                    isBuy = it.isBuy,
                    isPersonal = it.isPersonal,
                    location = details[it.locationId],
                    quantity = it.quantity,
                    transactionId = it.transactionId,
                    type = details[it.typeId],
                    unitPrice = it.unitPrice,
                )
            }
            WalletJournalItem(
                wallet = wallet,
                amount = entry.amount ?: 0.0,
                balance = entry.balance,
                context = details[entry.contextId],
                date = entry.date,
                description = entry.description,
                id = entry.id,
                reason = entry.reason,
                reasonTypeDetails = getAdditionalTypeDetails(entry.refType, entry.reason, entry.description),
                refType = entry.refType,
                firstParty = details[entry.firstPartyId],
                secondParty = details[entry.secondPartyId],
                tax = entry.tax,
                taxReceiver = details[entry.taxReceiverId],
                transaction = transaction,
            )
        }
        return items
    }

    private fun fixJournalEntries(journalEntries: List<WalletJournalEntry>): List<WalletJournalEntry> {
        return journalEntries.map { entry ->
            if (entry.refType == "insurance" && entry.firstPartyId == 2L) {
                // Insurance has a bug where the first party ID is set to 2 instead of the corporation ID of the EVE Central Bank
                entry.copy(firstPartyId = 98099645)
            } else {
                entry
            }
        }
    }

    private fun getAdditionalTypeDetails(
        referenceType: String,
        reason: String?,
        description: String,
    ): List<TypeDetail> {
        if (referenceType == "project_payouts" && reason != null) {
            val map = reason.split(":")
                .mapNotNull {
                    val parts = it.split("=")
                    if (parts.size == 2) parts else null
                }
                .associate { it[0] to it[1] }
            val goalId = map["goal_id"]
            val goalName = map["goal_name"]
            if (goalId != null && goalName != null) {
                return listOf(TypeDetail.CorporationProject(goalId, goalName))
            }
        } else if (referenceType == "freelance_jobs_reward" && reason != null) {
            val map = reason.split(":")
                .mapNotNull {
                    val parts = it.split("=")
                    if (parts.size == 2) parts else null
                }
                .associate { it[0] to it[1] }
            val projectId = map["project_id"]
            val projectName = map["project_name"]
            if (projectId != null && projectName != null) {
                return listOf(TypeDetail.FreelanceProject(projectId, projectName))
            }
        } else if (referenceType == "bounty_prizes" && reason != null) {
            return reason.split(",")
                .mapNotNull {
                    val parts = it.split(": ")
                    if (parts.size == 2) {
                        (parts[0].toIntOrNull() ?: return@mapNotNull null) to (parts[1].toLongOrNull() ?: return@mapNotNull null)
                    } else {
                        null
                    }
                }
                .mapNotNull { (typeId, count) ->
                    val type = typesRepository.getType(typeId) ?: return@mapNotNull null
                    TypeDetail.Type(type, count)
                }
        } else if (referenceType == "planetary_construction") {
            val planetName = description.substringAfter(" built on ")
            val planet = celestialsRepository.getCelestial(planetName)
            if (planet != null) {
                val system = solarSystemsRepository.getSystem(planet.solarSystemId)
                if (system != null) {
                    return listOf(TypeDetail.SolarSystem(system), TypeDetail.Celestial(planet))
                }
            }
        } else if (referenceType == "daily_goal_payouts" && reason != null) {
            when (reason.toIntOrNull()) {
                697658 -> "Scan 5 Signatures"
                697667 -> "Destroy 25 non-capsuleers"
                697670 -> "Mine 2000 units of Ore"
                697671 -> "Manufacture an Item"
                697674 -> "Damage other Capsuleers"
                697675 -> "Shield Boost other Capsuleers"
                697676 -> "Armor Repair other Capsuleers"
                697680 -> "Capture Contested FW Complex"
                697681 -> "Defend Contested FW Complex"
                712805 -> "Earn 50 LP for any corporation"
                712833 -> "Complete 9 Daily Bonus Goals - Alpha"
                1004953 -> "Complete 3 Jumps"
                else -> null
            }?.let { name ->
                return listOf(TypeDetail.DailyGoal(name))
            }
        }
        return emptyList()
    }

    /**
     * Loads details about all the types referenced in the journal entries and transactions.
     * characterId is for fetching structure details
     */
    private suspend fun loadTypeDetails(
        characterId: Int,
        journalEntries: List<WalletJournalEntry>,
        transactions: List<WalletTransaction>,
    ): TypeDetails {
        // Prepare lists of IDs to fetch details about
        val uncategorizedIds = mutableListOf<Long>()
        val structureIds = mutableListOf<Long>()
        val stationIds = mutableListOf<Long>()
        val characterIds = mutableListOf<Long>()
        val corporationIds = mutableListOf<Long>()
        val allianceIds = mutableListOf<Long>()
        val factionIds = mutableListOf<Long>()
        val systemIds = mutableListOf<Long>()
        val typeIds = mutableListOf<Long>()
        journalEntries.forEach { entry ->
            if (entry.contextId != null) {
                when (entry.contextIdType) {
                    ContextIdType.Structure -> structureIds += entry.contextId
                    ContextIdType.Station -> stationIds += entry.contextId
                    ContextIdType.MarketTransaction -> {} // We already have transactions, no need to fetch anything
                    ContextIdType.Character -> characterIds += entry.contextId
                    ContextIdType.Corporation -> corporationIds += entry.contextId
                    ContextIdType.Alliance -> allianceIds += entry.contextId
                    ContextIdType.EveSystem -> uncategorizedIds += entry.contextId
                    ContextIdType.IndustryJob -> {} // Not supported
                    ContextIdType.Contract -> {} // Not supported
                    ContextIdType.Planet -> {} // Not supported
                    ContextIdType.System -> systemIds += entry.contextId
                    ContextIdType.Type -> typeIds += entry.contextId
                    null -> {}
                }
            }
            uncategorizedIds += listOfNotNull(entry.firstPartyId, entry.secondPartyId)
            if (entry.taxReceiverId != null) corporationIds += entry.taxReceiverId
        }
        transactions.forEach { transaction ->
            uncategorizedIds += transaction.clientId
            if (IdRanges.isStation(transaction.locationId)) {
                stationIds += transaction.locationId
            } else {
                structureIds += transaction.locationId
            }
            typeIds += transaction.typeId
        }

        // Spawned items are not supported by ESI for name/category resolution
        val spawnedItemIds = uncategorizedIds.filter { IdRanges.isSpawnedItem(it) }.toSet()
        uncategorizedIds -= spawnedItemIds
        structureIds += spawnedItemIds

        // For those IDs where we don't know the category, fetch the categories from ESI
        namesRepository.resolveNames(uncategorizedIds)
        uncategorizedIds.forEach { id ->
            when (namesRepository.getCategory(id)) {
                UniverseNamesCategory.Character -> characterIds += id
                UniverseNamesCategory.Constellation -> {}
                UniverseNamesCategory.Corporation -> corporationIds += id
                UniverseNamesCategory.InventoryType -> typeIds += id
                UniverseNamesCategory.Region -> {}
                UniverseNamesCategory.SolarSystem -> systemIds += id
                UniverseNamesCategory.Station -> stationIds += id
                UniverseNamesCategory.Faction -> factionIds += id
                UniverseNamesCategory.Alliance -> allianceIds += id
                null -> {}
            }
        }

        // Fetch details about all the IDs
        val details = mutableMapOf<Long, TypeDetail>()
        coroutineScope {
            val deferredStructures = structureIds.distinct().map {
                async {
                    locationRepository.getStructure(it, characterId, fetchOwner = true)
                }
            }
            val deferredStations = stationIds.distinct().map {
                async {
                    locationRepository.getStation(it.toInt(), fetchOwner = true)
                }
            }
            val deferredCharacters = characterIds.distinct().let {
                async {
                    characterDetailsRepository.getCharacterDetails(it.map(Long::toInt)).values
                }
            }
            val deferredCorporations = corporationIds.distinct().map { corporationId ->
                async {
                    characterDetailsRepository.getCorporationDetails(corporationId.toInt())
                }
            }
            val deferredAlliances = allianceIds.distinct().map {
                async {
                    characterDetailsRepository.getAllianceDetails(it.toInt())
                }
            }

            deferredStructures.awaitAll().forEach { structure ->
                structure?.let { details[it.structureId] = TypeDetail.Structure(it) }
            }
            deferredStations.awaitAll().forEach { station ->
                station?.let { details[it.stationId.toLong()] = TypeDetail.Station(it) }
            }
            deferredCharacters.await().forEach { character ->
                character?.let { details[it.characterId.toLong()] = TypeDetail.Character(it) }
            }
            deferredCorporations.awaitAll().forEach { corporation ->
                corporation?.let { details[corporation.corporationId.toLong()] = TypeDetail.Corporation(corporation) }
            }
            deferredAlliances.awaitAll().forEach { alliance ->
                alliance?.let { details[alliance.allianceId.toLong()] = TypeDetail.Alliance(alliance) }
            }
            factionIds.distinct().map { id ->
                details[id] = TypeDetail.Faction(id, FactionNames[id.toInt()])
            }
            systemIds.distinct().mapNotNull { id ->
                solarSystemsRepository.getSystem(id.toInt())?.let { details[id] = TypeDetail.SolarSystem(it) }
            }
            typeIds.distinct().mapNotNull { id ->
                typesRepository.getType(id.toInt())?.let { details[id] = TypeDetail.Type(it) }
            }
        }

        return TypeDetails(details)
    }
}
