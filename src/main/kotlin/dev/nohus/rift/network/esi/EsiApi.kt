package dev.nohus.rift.network.esi

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.nohus.rift.network.RequestExecutor
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
import dev.nohus.rift.network.esi.models.CorporationProjectsQueryState
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
import dev.nohus.rift.sso.scopes.EsiScope
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.Response
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

    suspend fun postUniverseIds(names: List<String>): Result<UniverseIdsResponse> {
        return execute { service.postUniverseIds(names) }
    }

    suspend fun postUniverseNames(ids: List<Int>): Result<List<UniverseName>> {
        return execute { service.postUniverseNames(ids) }
    }

    suspend fun getCharactersId(characterId: Int): Result<CharactersIdCharacter> {
        return execute { service.getCharactersId(characterId) }
    }

    suspend fun getCharactersAffiliation(characterIds: List<Int>): Result<List<CharactersAffiliation>> {
        return execute { service.getCharactersAffiliation(characterIds) }
    }

    suspend fun getCorporationsId(corporationId: Int): Result<CorporationsIdCorporation> {
        return execute { service.getCorporationsId(corporationId) }
    }

    suspend fun getAlliancesId(allianceId: Int): Result<AlliancesIdAlliance> {
        return execute { service.getAlliancesId(allianceId) }
    }

    suspend fun getAlliancesIdContacts(characterId: Int, allianceId: Int): Result<List<Contact>> {
        return executeEveAuthorized(characterId, EsiScope.Alliances.ReadContacts) { authorization ->
            service.getAlliancesIdContacts(allianceId, authorization)
        }
    }

    suspend fun getCorporationsIdContacts(characterId: Int, corporationId: Int): Result<List<Contact>> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadContacts) { authorization ->
            service.getCorporationsIdContacts(corporationId, authorization)
        }
    }

    suspend fun getCharactersIdContacts(characterId: Int): Result<List<Contact>> {
        return executeEveAuthorized(characterId, EsiScope.Characters.ReadContacts) { authorization ->
            service.getCharactersIdContacts(characterId, authorization)
        }
    }

    suspend fun getAlliancesIdContactsLabels(characterId: Int, allianceId: Int): Result<List<ContactsLabel>> {
        return executeEveAuthorized(characterId, EsiScope.Alliances.ReadContacts) { authorization ->
            service.getAlliancesIdContactsLabels(allianceId, authorization)
        }
    }

    suspend fun getCorporationsIdContactsLabels(characterId: Int, corporationId: Int): Result<List<ContactsLabel>> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadContacts) { authorization ->
            service.getCorporationsIdContactsLabels(corporationId, authorization)
        }
    }

    suspend fun getCharactersIdContactsLabels(characterId: Int): Result<List<ContactsLabel>> {
        return executeEveAuthorized(characterId, EsiScope.Characters.ReadContacts) { authorization ->
            service.getCharactersIdContactsLabels(characterId, authorization)
        }
    }

    suspend fun deleteCharactersIdContacts(
        characterId: Int,
        contactIds: List<Int>,
    ): Result<Unit> {
        return executeEveAuthorized(characterId, EsiScope.Characters.WriteContacts) { authorization ->
            service.deleteCharactersIdContacts(
                characterId = characterId,
                contactIds = contactIds,
                authorization = authorization,
            )
        }
    }

    suspend fun postCharactersIdContacts(
        characterId: Int,
        labelIds: List<Long>?,
        standing: Float,
        watched: Boolean?,
        contactIds: List<Int>,
    ): Result<List<Int>> {
        return executeEveAuthorized(characterId, EsiScope.Characters.WriteContacts) { authorization ->
            service.postCharactersIdContacts(
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
        characterId: Int,
        labelIds: List<Long>?,
        standing: Float,
        watched: Boolean?,
        contactIds: List<Int>,
    ): Result<Unit> {
        return executeEveAuthorized(characterId, EsiScope.Characters.WriteContacts) { authorization ->
            service.putCharactersIdContacts(characterId, labelIds, standing, watched, authorization, contactIds)
        }
    }

    suspend fun getCharacterIdOnline(characterId: Int): Result<CharacterIdOnline> {
        return executeEveAuthorized(characterId, EsiScope.Locations.ReadOnline) { authorization ->
            service.getCharacterIdOnline(characterId, authorization)
        }
    }

    suspend fun getCharacterIdShip(characterId: Int): Result<CharacterIdShip> {
        return executeEveAuthorized(characterId, EsiScope.Locations.ReadShipType) { authorization ->
            service.getCharacterIdShip(characterId, authorization)
        }
    }

    suspend fun getCharacterIdLocation(characterId: Int): Result<CharacterIdLocation> {
        return executeEveAuthorized(characterId, EsiScope.Locations.ReadLocation) { authorization ->
            service.getCharacterIdLocation(characterId, authorization)
        }
    }

    suspend fun getCharacterIdWallet(characterId: Int): Result<Double> {
        return executeEveAuthorized(characterId, EsiScope.Wallet.ReadCharacterWallet) { authorization ->
            service.getCharactersIdWallet(characterId, authorization)
        }
    }

    suspend fun getCharactersIdSearch(characterId: Int, categories: List<String>, strict: Boolean, search: String): Result<CharactersIdSearch> {
        return executeEveAuthorized(characterId, EsiScope.Search.SearchStructures) { authorization ->
            service.getCharactersIdSearch(characterId, categories, strict, search, authorization)
        }
    }

    suspend fun getCharactersIdClones(characterId: Int): Result<CharactersIdClones> {
        return executeEveAuthorized(characterId, EsiScope.Clones.ReadClones) { authorization ->
            service.getCharactersIdClones(characterId, authorization)
        }
    }

    suspend fun getCharactersIdImplants(characterId: Int): Result<List<Int>> {
        return executeEveAuthorized(characterId, EsiScope.Clones.ReadImplants) { authorization ->
            service.getCharactersIdImplants(characterId, authorization)
        }
    }

    suspend fun getUniverseStationsId(stationId: Int): Result<UniverseStationsId> {
        return execute { service.getUniverseStationsId(stationId) }
    }

    suspend fun getUniverseStructuresId(structureId: Long, characterId: Int): Result<UniverseStructuresId> {
        return executeEveAuthorized(characterId, EsiScope.Universe.ReadStructures) { authorization ->
            service.getUniverseStructuresId(structureId, authorization)
        }
    }

    suspend fun getUniverseSystemJumps(): Result<List<UniverseSystemJumps>> {
        return execute { service.getUniverseSystemJumps() }
    }

    suspend fun getUniverseSystemKills(): Result<List<UniverseSystemKills>> {
        return execute { service.getUniverseSystemKills() }
    }

    suspend fun getIncursions(): Result<List<Incursion>> {
        return execute { service.getIncursions() }
    }

    suspend fun getFactionWarfareSystems(): Result<List<FactionWarfareSystem>> {
        return execute { service.getFactionWarfareSystems() }
    }

    suspend fun getSovereigntyMap(): Result<List<SovereigntySystem>> {
        return execute { service.getSovereigntyMap() }
    }

    suspend fun postUiAutopilotWaypoint(
        destinationId: Long,
        clearOtherWaypoints: Boolean,
        characterId: Int,
    ): Result<Unit> {
        return executeEveAuthorized(characterId, EsiScope.Ui.WriteWaypoint) { authorization ->
            service.postUiAutopilotWaypoint(
                addToBeginning = false,
                clearOtherWaypoints = clearOtherWaypoints,
                destinationId = destinationId,
                authorization = authorization,
            )
        }
    }

    suspend fun getCharactersIdAssets(page: Int, characterId: Int): Result<Response<List<CharactersIdAsset>>> {
        return executeEveAuthorized(characterId, EsiScope.Assets.ReadAssets) { authorization ->
            service.getCharactersIdAssets(characterId, page, authorization)
        }
    }

    suspend fun getCharactersIdAssetsNames(characterId: Int, assets: List<Long>): Result<List<CharactersIdAssetsName>> {
        return executeEveAuthorized(characterId, EsiScope.Assets.ReadAssets) { authorization ->
            service.getCharactersIdAssetsNames(characterId, assets, authorization)
        }
    }

    suspend fun getCharactersIdAssetsLocations(characterId: Int, itemIds: List<Long>): Result<List<CharactersIdAssetsLocation>> {
        return executeEveAuthorized(characterId, EsiScope.Assets.ReadAssets) { authorization ->
            service.getCharactersIdAssetsLocations(characterId, itemIds, authorization)
        }
    }

    suspend fun getMarketsPrices(): Result<List<MarketsPrice>> {
        return execute { service.getMarketsPrices() }
    }

    suspend fun getCharactersIdFleet(characterId: Int): Result<CharactersIdFleet> {
        return executeEveAuthorized(characterId, EsiScope.Fleets.ReadFleet) { authorization ->
            service.getCharactersIdFleet(characterId, authorization)
        }
    }

    suspend fun getFleetsId(characterId: Int, fleetId: Long): Result<FleetsId> {
        return executeEveAuthorized(characterId, EsiScope.Fleets.ReadFleet) { authorization ->
            service.getFleetsId(fleetId, authorization)
        }
    }

    suspend fun getFleetsIdMembers(characterId: Int, fleetId: Long): Result<List<FleetMember>> {
        return executeEveAuthorized(characterId, EsiScope.Fleets.ReadFleet) { authorization ->
            service.getFleetsIdMembers(fleetId, authorization)
        }
    }

    suspend fun getCharactersIdPlanets(characterId: Int): Result<List<CharactersIdPlanet>> {
        return executeEveAuthorized(characterId, EsiScope.Planets.ManagePlanets) { authorization ->
            service.getCharactersIdPlanets(characterId, authorization)
        }
    }

    suspend fun getCharactersIdPlanetsId(characterId: Int, planetId: Int): Result<CharactersIdPlanetsId> {
        return executeEveAuthorized(characterId, EsiScope.Planets.ManagePlanets) { authorization ->
            service.getCharactersIdPlanetsId(characterId, planetId, authorization)
        }
    }

    suspend fun getIndustrySystems(): Result<List<IndustrySystem>> {
        return execute { service.getIndustrySystems() }
    }

    suspend fun getCorporationsIdProjects(
        characterId: Int,
        corporationId: Int,
        before: String?,
        after: String?,
        limit: Int? = 100,
        state: CorporationProjectsQueryState?,
    ): Result<CorporationsIdProjects> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadProjects) { authorization ->
            service.getCorporationsIdProjects(corporationId, before, after, limit, state, authorization)
        }
    }

    suspend fun getCorporationsIdProjectsId(
        characterId: Int,
        corporationId: Int,
        projectId: String,
    ): Result<CorporationsIdProjectsId> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadProjects) { authorization ->
            service.getCorporationsIdProjectsId(corporationId, projectId, authorization)
        }
    }

    suspend fun getCorporationsIdProjectsIdContribution(
        characterId: Int,
        corporationId: Int,
        projectId: String,
    ): Result<CorporationsIdProjectsIdContribution> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadProjects) { authorization ->
            service.getCorporationsIdProjectsIdContribution(corporationId, projectId, characterId, authorization)
        }
    }

    suspend fun getCorporationsIdProjectsIdContributors(
        characterId: Int,
        corporationId: Int,
        projectId: String,
        before: String?,
        after: String?,
        limit: Int? = 100,
    ): Result<CorporationsIdProjectsIdContributors> {
        return executeEveAuthorized(characterId, EsiScope.Corporations.ReadProjects) { authorization ->
            service.getCorporationsIdProjectsIdContributors(corporationId, projectId, before, after, limit, authorization)
        }
    }

    suspend fun getCharactersIdRoles(
        characterId: Int,
    ): Result<CharactersIdRoles> {
        return executeEveAuthorized(characterId, EsiScope.Characters.ReadCorporationRoles) { authorization ->
            service.getCharactersIdRoles(characterId, authorization)
        }
    }

    suspend fun postUiOpenWindowInformation(
        characterId: Int,
        id: Long,
    ): Result<Unit> {
        return executeEveAuthorized(characterId, EsiScope.Ui.OpenWindow) { authorization ->
            service.postUiOpenWindowInformation(id, authorization)
        }
    }

    suspend fun postUiOpenWindowMarketDetails(
        characterId: Int,
        typeId: Long,
    ): Result<Unit> {
        return executeEveAuthorized(characterId, EsiScope.Ui.OpenWindow) { authorization ->
            service.postUiOpenWindowMarketDetails(typeId, authorization)
        }
    }

    suspend fun postUiOpenWindowNewMail(
        characterId: Int,
        request: NewMailRequest,
    ): Result<Unit> {
        return executeEveAuthorized(characterId, EsiScope.Ui.OpenWindow) { authorization ->
            service.postUiOpenWindowNewMail(request, authorization)
        }
    }
}
