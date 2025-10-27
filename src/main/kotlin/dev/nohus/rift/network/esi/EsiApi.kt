package dev.nohus.rift.network.esi

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.nohus.rift.network.Result
import dev.nohus.rift.network.esi.models.AlliancesIdAlliance
import dev.nohus.rift.network.esi.models.CharacterIdLocation
import dev.nohus.rift.network.esi.models.CharacterIdOnline
import dev.nohus.rift.network.esi.models.CharacterIdShip
import dev.nohus.rift.network.esi.models.CharactersAffiliation
import dev.nohus.rift.network.esi.models.CharactersIdAsset
import dev.nohus.rift.network.esi.models.CharactersIdAssetsLocation
import dev.nohus.rift.network.esi.models.CharactersIdAssetsName
import dev.nohus.rift.network.esi.models.CharactersIdCharacter
import dev.nohus.rift.network.esi.models.CharactersIdClones
import dev.nohus.rift.network.esi.models.CharactersIdFleet
import dev.nohus.rift.network.esi.models.CharactersIdPlanet
import dev.nohus.rift.network.esi.models.CharactersIdPlanetsId
import dev.nohus.rift.network.esi.models.CharactersIdRoles
import dev.nohus.rift.network.esi.models.CharactersIdSearch
import dev.nohus.rift.network.esi.models.Contact
import dev.nohus.rift.network.esi.models.ContactsLabel
import dev.nohus.rift.network.esi.models.CorporationDivisions
import dev.nohus.rift.network.esi.models.CorporationProjectsQueryState
import dev.nohus.rift.network.esi.models.CorporationWalletBalance
import dev.nohus.rift.network.esi.models.CorporationsIdCorporation
import dev.nohus.rift.network.esi.models.CorporationsIdProjects
import dev.nohus.rift.network.esi.models.CorporationsIdProjectsId
import dev.nohus.rift.network.esi.models.CorporationsIdProjectsIdContribution
import dev.nohus.rift.network.esi.models.CorporationsIdProjectsIdContributors
import dev.nohus.rift.network.esi.models.FactionWarfareSystem
import dev.nohus.rift.network.esi.models.FleetMember
import dev.nohus.rift.network.esi.models.FleetsId
import dev.nohus.rift.network.esi.models.Incursion
import dev.nohus.rift.network.esi.models.IndustrySystem
import dev.nohus.rift.network.esi.models.MarketsPrice
import dev.nohus.rift.network.esi.models.NewMailRequest
import dev.nohus.rift.network.esi.models.SovereigntySystem
import dev.nohus.rift.network.esi.models.UniverseIdsResponse
import dev.nohus.rift.network.esi.models.UniverseName
import dev.nohus.rift.network.esi.models.UniverseStationsId
import dev.nohus.rift.network.esi.models.UniverseStructuresId
import dev.nohus.rift.network.esi.models.UniverseSystemJumps
import dev.nohus.rift.network.esi.models.UniverseSystemKills
import dev.nohus.rift.network.esi.models.WalletJournalEntry
import dev.nohus.rift.network.esi.models.WalletTransaction
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.network.requests.Reply
import dev.nohus.rift.network.requests.RequestExecutor
import dev.nohus.rift.sso.scopes.EsiScope
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.Retrofit

@Single
class EsiApi(
    @Named("network") json: Json,
    @Named("esi") client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://esi.evetech.net/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
    private val service = retrofit.create(EsiService::class.java)

    suspend fun postUniverseIds(originator: Originator, names: List<String>): Result<UniverseIdsResponse> {
        return execute { service.postUniverseIds(originator, names) }
    }

    suspend fun postUniverseNames(originator: Originator, ids: List<Long>): Result<List<UniverseName>> {
        return execute { service.postUniverseNames(originator, ids) }
    }

    suspend fun getCharactersId(originator: Originator, characterId: Int): Result<CharactersIdCharacter> {
        return execute { service.getCharactersId(originator, characterId) }
    }

    suspend fun getCharactersAffiliation(originator: Originator, characterIds: List<Int>): Result<List<CharactersAffiliation>> {
        return execute { service.getCharactersAffiliation(originator, characterIds) }
    }

    suspend fun getCorporationsId(originator: Originator, corporationId: Int): Result<CorporationsIdCorporation> {
        return execute { service.getCorporationsId(originator, corporationId) }
    }

    suspend fun getAlliancesId(originator: Originator, allianceId: Int): Result<AlliancesIdAlliance> {
        return execute { service.getAlliancesId(originator, allianceId) }
    }

    suspend fun getAlliancesIdContacts(originator: Originator, characterId: Int, allianceId: Int): Result<List<Contact>> {
        return executeEveAuthorized(characterId, EsiScope.Alliances.ReadContacts) { authorization ->
            service.getAlliancesIdContacts(originator, allianceId, authorization)
        }
    }

    suspend fun getCorporationsIdContacts(originator: Originator, characterId: Int, corporationId: Int): Result<List<Contact>> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadContacts) { authorization ->
            service.getCorporationsIdContacts(originator, corporationId, authorization)
        }
    }

    suspend fun getCharactersIdContacts(originator: Originator, characterId: Int): Result<List<Contact>> {
        return executeEveAuthorized(characterId, EsiScope.Characters.ReadContacts) { authorization ->
            service.getCharactersIdContacts(originator, characterId, authorization)
        }
    }

    suspend fun getAlliancesIdContactsLabels(originator: Originator, characterId: Int, allianceId: Int): Result<List<ContactsLabel>> {
        return executeEveAuthorized(characterId, EsiScope.Alliances.ReadContacts) { authorization ->
            service.getAlliancesIdContactsLabels(originator, allianceId, authorization)
        }
    }

    suspend fun getCorporationsIdContactsLabels(originator: Originator, characterId: Int, corporationId: Int): Result<List<ContactsLabel>> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadContacts) { authorization ->
            service.getCorporationsIdContactsLabels(originator, corporationId, authorization)
        }
    }

    suspend fun getCharactersIdContactsLabels(originator: Originator, characterId: Int): Result<List<ContactsLabel>> {
        return executeEveAuthorized(characterId, EsiScope.Characters.ReadContacts) { authorization ->
            service.getCharactersIdContactsLabels(originator, characterId, authorization)
        }
    }

    suspend fun deleteCharactersIdContacts(
        originator: Originator,
        characterId: Int,
        contactIds: List<Int>,
    ): Result<Unit> {
        return executeEveAuthorized(characterId, EsiScope.Characters.WriteContacts) { authorization ->
            service.deleteCharactersIdContacts(
                originator = originator,
                characterId = characterId,
                contactIds = contactIds,
                authorization = authorization,
            )
        }
    }

    suspend fun postCharactersIdContacts(
        originator: Originator,
        characterId: Int,
        labelIds: List<Long>?,
        standing: Float,
        watched: Boolean?,
        contactIds: List<Int>,
    ): Result<List<Int>> {
        return executeEveAuthorized(characterId, EsiScope.Characters.WriteContacts) { authorization ->
            service.postCharactersIdContacts(
                originator = originator,
                characterId = characterId,
                labelIds = labelIds,
                standing = standing,
                watched = watched,
                authorization = authorization,
                contactIds = contactIds,
            )
        }
    }

    suspend fun putCharactersIdContacts(
        originator: Originator,
        characterId: Int,
        labelIds: List<Long>?,
        standing: Float,
        watched: Boolean?,
        contactIds: List<Int>,
    ): Result<Unit> {
        return executeEveAuthorized(characterId, EsiScope.Characters.WriteContacts) { authorization ->
            service.putCharactersIdContacts(originator, characterId, labelIds, standing, watched, authorization, contactIds)
        }
    }

    suspend fun getCharacterIdOnline(originator: Originator, characterId: Int): Result<CharacterIdOnline> {
        return executeEveAuthorized(characterId, EsiScope.Locations.ReadOnline) { authorization ->
            service.getCharacterIdOnline(originator, characterId, authorization)
        }
    }

    suspend fun getCharacterIdShip(originator: Originator, characterId: Int): Result<CharacterIdShip> {
        return executeEveAuthorized(characterId, EsiScope.Locations.ReadShipType) { authorization ->
            service.getCharacterIdShip(originator, characterId, authorization)
        }
    }

    suspend fun getCharacterIdLocation(originator: Originator, characterId: Int): Result<CharacterIdLocation> {
        return executeEveAuthorized(characterId, EsiScope.Locations.ReadLocation) { authorization ->
            service.getCharacterIdLocation(originator, characterId, authorization)
        }
    }

    suspend fun getCharacterIdWallet(originator: Originator, characterId: Int): Result<Double> {
        return executeEveAuthorized(characterId, EsiScope.Wallet.ReadCharacterWallet) { authorization ->
            service.getCharactersIdWallet(originator, characterId, authorization)
        }
    }

    suspend fun getCharactersIdWalletJournal(
        originator: Originator,
        characterId: Int,
        page: Int? = null,
    ): Result<Reply<List<WalletJournalEntry>>> {
        return executeEveAuthorizedWithHeaders(characterId, EsiScope.Wallet.ReadCharacterWallet) { authorization ->
            service.getCharactersIdWalletJournal(originator, characterId, page, authorization)
        }
    }

    suspend fun getCharactersIdWalletTransactions(
        originator: Originator,
        characterId: Int,
        fromId: Long? = null,
    ): Result<List<WalletTransaction>> {
        return executeEveAuthorized(characterId, EsiScope.Wallet.ReadCharacterWallet) { authorization ->
            service.getCharactersIdWalletTransactions(originator, characterId, fromId, authorization)
        }
    }

    suspend fun getCorporationsCorporationIdWallet(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
    ): Result<List<CorporationWalletBalance>> {
        return executeEveAuthorized(characterId, EsiScope.Wallet.ReadCorporationWallets) { authorization ->
            service.getCorporationsCorporationIdWallets(originator, corporationId, authorization)
        }
    }

    suspend fun getCorporationsCorporationIdWalletsDivisionJournal(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        division: Int,
        page: Int? = null,
    ): Result<Reply<List<WalletJournalEntry>>> {
        return executeEveAuthorizedWithHeaders(characterId, EsiScope.Wallet.ReadCorporationWallets) { authorization ->
            service.getCorporationsCorporationIdWalletsDivisionJournal(originator, corporationId, division, page, authorization)
        }
    }

    suspend fun getCorporationsCorporationIdWalletsDivisionTransactions(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        division: Int,
        fromId: Long? = null,
    ): Result<List<WalletTransaction>> {
        return executeEveAuthorized(characterId, EsiScope.Wallet.ReadCorporationWallets) { authorization ->
            service.getCorporationsCorporationIdWalletsDivisionTransactions(originator, corporationId, division, fromId, authorization)
        }
    }

    suspend fun getCorporationsCorporationIdDivisions(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
    ): Result<CorporationDivisions> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadDivisions) { authorization ->
            service.getCorporationsCorporationIdDivisions(originator, corporationId, authorization)
        }
    }

    suspend fun getCharactersIdSearch(originator: Originator, characterId: Int, categories: List<String>, strict: Boolean, search: String): Result<CharactersIdSearch> {
        return executeEveAuthorized(characterId, EsiScope.Search.SearchStructures) { authorization ->
            service.getCharactersIdSearch(originator, characterId, categories, strict, search, authorization)
        }
    }

    suspend fun getCharactersIdClones(originator: Originator, characterId: Int): Result<CharactersIdClones> {
        return executeEveAuthorized(characterId, EsiScope.Clones.ReadClones) { authorization ->
            service.getCharactersIdClones(originator, characterId, authorization)
        }
    }

    suspend fun getCharactersIdImplants(originator: Originator, characterId: Int): Result<List<Int>> {
        return executeEveAuthorized(characterId, EsiScope.Clones.ReadImplants) { authorization ->
            service.getCharactersIdImplants(originator, characterId, authorization)
        }
    }

    suspend fun getUniverseStationsId(originator: Originator, stationId: Int): Result<UniverseStationsId> {
        return execute { service.getUniverseStationsId(originator, stationId) }
    }

    suspend fun getUniverseStructuresId(originator: Originator, structureId: Long, characterId: Int): Result<UniverseStructuresId> {
        return executeEveAuthorized(characterId, EsiScope.Universe.ReadStructures) { authorization ->
            service.getUniverseStructuresId(originator, structureId, authorization)
        }
    }

    suspend fun getUniverseSystemJumps(originator: Originator): Result<List<UniverseSystemJumps>> {
        return execute { service.getUniverseSystemJumps(originator) }
    }

    suspend fun getUniverseSystemKills(originator: Originator): Result<List<UniverseSystemKills>> {
        return execute { service.getUniverseSystemKills(originator) }
    }

    suspend fun getIncursions(originator: Originator): Result<List<Incursion>> {
        return execute { service.getIncursions(originator) }
    }

    suspend fun getFactionWarfareSystems(originator: Originator): Result<List<FactionWarfareSystem>> {
        return execute { service.getFactionWarfareSystems(originator) }
    }

    suspend fun getSovereigntyMap(originator: Originator): Result<List<SovereigntySystem>> {
        return execute { service.getSovereigntyMap(originator) }
    }

    suspend fun postUiAutopilotWaypoint(
        originator: Originator,
        destinationId: Long,
        clearOtherWaypoints: Boolean,
        characterId: Int,
    ): Result<Unit> {
        return executeEveAuthorized(characterId, EsiScope.Ui.WriteWaypoint) { authorization ->
            service.postUiAutopilotWaypoint(
                originator = originator,
                addToBeginning = false,
                clearOtherWaypoints = clearOtherWaypoints,
                destinationId = destinationId,
                authorization = authorization,
            )
        }
    }

    suspend fun getCharactersIdAssets(originator: Originator, page: Int, characterId: Int): Result<Reply<List<CharactersIdAsset>>> {
        return executeEveAuthorizedWithHeaders(characterId, EsiScope.Assets.ReadAssets) { authorization ->
            service.getCharactersIdAssets(originator, characterId, page, authorization)
        }
    }

    suspend fun getCharactersIdAssetsNames(originator: Originator, characterId: Int, assets: List<Long>): Result<List<CharactersIdAssetsName>> {
        return executeEveAuthorized(characterId, EsiScope.Assets.ReadAssets) { authorization ->
            service.getCharactersIdAssetsNames(originator, characterId, assets, authorization)
        }
    }

    suspend fun getCharactersIdAssetsLocations(originator: Originator, characterId: Int, itemIds: List<Long>): Result<List<CharactersIdAssetsLocation>> {
        return executeEveAuthorized(characterId, EsiScope.Assets.ReadAssets) { authorization ->
            service.getCharactersIdAssetsLocations(originator, characterId, itemIds, authorization)
        }
    }

    suspend fun getMarketsPrices(originator: Originator): Result<List<MarketsPrice>> {
        return execute { service.getMarketsPrices(originator) }
    }

    suspend fun getCharactersIdFleet(originator: Originator, characterId: Int): Result<CharactersIdFleet> {
        return executeEveAuthorized(characterId, EsiScope.Fleets.ReadFleet) { authorization ->
            service.getCharactersIdFleet(originator, characterId, authorization)
        }
    }

    suspend fun getFleetsId(originator: Originator, characterId: Int, fleetId: Long): Result<FleetsId> {
        return executeEveAuthorized(characterId, EsiScope.Fleets.ReadFleet) { authorization ->
            service.getFleetsId(originator, fleetId, authorization)
        }
    }

    suspend fun getFleetsIdMembers(originator: Originator, characterId: Int, fleetId: Long): Result<List<FleetMember>> {
        return executeEveAuthorized(characterId, EsiScope.Fleets.ReadFleet) { authorization ->
            service.getFleetsIdMembers(originator, fleetId, authorization)
        }
    }

    suspend fun getCharactersIdPlanets(originator: Originator, characterId: Int): Result<List<CharactersIdPlanet>> {
        return executeEveAuthorized(characterId, EsiScope.Planets.ManagePlanets) { authorization ->
            service.getCharactersIdPlanets(originator, characterId, authorization)
        }
    }

    suspend fun getCharactersIdPlanetsId(originator: Originator, characterId: Int, planetId: Int): Result<CharactersIdPlanetsId> {
        return executeEveAuthorized(characterId, EsiScope.Planets.ManagePlanets) { authorization ->
            service.getCharactersIdPlanetsId(originator, characterId, planetId, authorization)
        }
    }

    suspend fun getIndustrySystems(originator: Originator): Result<List<IndustrySystem>> {
        return execute { service.getIndustrySystems(originator) }
    }

    suspend fun getCorporationsIdProjects(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        before: String?,
        after: String?,
        limit: Int? = 100,
        state: CorporationProjectsQueryState?,
    ): Result<CorporationsIdProjects> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadProjects) { authorization ->
            service.getCorporationsIdProjects(originator, corporationId, before, after, limit, state, authorization)
        }
    }

    suspend fun getCorporationsIdProjectsId(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        projectId: String,
    ): Result<CorporationsIdProjectsId> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadProjects) { authorization ->
            service.getCorporationsIdProjectsId(originator, corporationId, projectId, authorization)
        }
    }

    suspend fun getCorporationsIdProjectsIdContribution(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        projectId: String,
    ): Result<CorporationsIdProjectsIdContribution> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadProjects) { authorization ->
            service.getCorporationsIdProjectsIdContribution(originator, corporationId, projectId, characterId, authorization)
        }
    }

    suspend fun getCorporationsIdProjectsIdContributors(
        originator: Originator,
        characterId: Int,
        corporationId: Int,
        projectId: String,
        before: String?,
        after: String?,
        limit: Int? = 100,
    ): Result<CorporationsIdProjectsIdContributors> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadProjects) { authorization ->
            service.getCorporationsIdProjectsIdContributors(originator, corporationId, projectId, before, after, limit, authorization)
        }
    }

    suspend fun getCharactersIdRoles(
        originator: Originator,
        characterId: Int,
    ): Result<CharactersIdRoles> {
        return executeEveAuthorized(characterId, EsiScope.Characters.ReadCorporationRoles) { authorization ->
            service.getCharactersIdRoles(originator, characterId, authorization)
        }
    }

    suspend fun postUiOpenWindowInformation(
        originator: Originator,
        characterId: Int,
        id: Long,
    ): Result<Unit> {
        return executeEveAuthorized(characterId, EsiScope.Ui.OpenWindow) { authorization ->
            service.postUiOpenWindowInformation(originator, id, authorization)
        }
    }

    suspend fun postUiOpenWindowMarketDetails(
        originator: Originator,
        characterId: Int,
        typeId: Long,
    ): Result<Unit> {
        return executeEveAuthorized(characterId, EsiScope.Ui.OpenWindow) { authorization ->
            service.postUiOpenWindowMarketDetails(originator, typeId, authorization)
        }
    }

    suspend fun postUiOpenWindowNewMail(
        originator: Originator,
        characterId: Int,
        request: NewMailRequest,
    ): Result<Unit> {
        return executeEveAuthorized(characterId, EsiScope.Ui.OpenWindow) { authorization ->
            service.postUiOpenWindowNewMail(originator, request, authorization)
        }
    }
}
