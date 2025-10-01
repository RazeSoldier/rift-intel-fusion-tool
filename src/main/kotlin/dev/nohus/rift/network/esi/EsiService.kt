package dev.nohus.rift.network.esi

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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface EsiService {

    @POST("/universe/ids")
    suspend fun postUniverseIds(
        @Body names: List<String>,
    ): UniverseIdsResponse

    @POST("/universe/names/")
    suspend fun postUniverseNames(
        @Body ids: List<Int>,
    ): List<UniverseName>

    @GET("/characters/{id}")
    suspend fun getCharactersId(
        @Path("id") characterId: Int,
    ): CharactersIdCharacter

    @POST("/characters/affiliation")
    suspend fun getCharactersAffiliation(
        @Body characterIds: List<Int>,
    ): List<CharactersAffiliation>

    @GET("/corporations/{id}")
    suspend fun getCorporationsId(
        @Path("id") corporationId: Int,
    ): CorporationsIdCorporation

    @GET("/alliances/{id}")
    suspend fun getAlliancesId(
        @Path("id") allianceId: Int,
    ): AlliancesIdAlliance

    @GET("/alliances/{id}/contacts/")
    suspend fun getAlliancesIdContacts(
        @Path("id") allianceId: Int,
        @Header("Authorization") authorization: String,
    ): List<Contact>

    @GET("/corporations/{id}/contacts/")
    suspend fun getCorporationsIdContacts(
        @Path("id") corporationId: Int,
        @Header("Authorization") authorization: String,
    ): List<Contact>

    @GET("/characters/{id}/contacts/")
    suspend fun getCharactersIdContacts(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): List<Contact>

    @GET("/alliances/{id}/contacts/labels/")
    suspend fun getAlliancesIdContactsLabels(
        @Path("id") allianceId: Int,
        @Header("Authorization") authorization: String,
    ): List<ContactsLabel>

    @GET("/corporations/{id}/contacts/labels/")
    suspend fun getCorporationsIdContactsLabels(
        @Path("id") corporationId: Int,
        @Header("Authorization") authorization: String,
    ): List<ContactsLabel>

    @GET("/characters/{id}/contacts/labels/")
    suspend fun getCharactersIdContactsLabels(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): List<ContactsLabel>

    @DELETE("/characters/{id}/contacts/")
    suspend fun deleteCharactersIdContacts(
        @Path("id") characterId: Int,
        @Query("contact_ids") contactIds: List<Int>,
        @Header("Authorization") authorization: String,
    )

    @POST("/characters/{id}/contacts/")
    suspend fun postCharactersIdContacts(
        @Path("id") characterId: Int,
        @Query("label_ids") labelIds: List<Long>?,
        @Query("standing") standing: Float,
        @Query("watched") watched: Boolean?,
        @Header("Authorization") authorization: String,
        @Body contactIds: List<Int>,
    ): List<Int>

    @PUT("/characters/{id}/contacts/")
    suspend fun putCharactersIdContacts(
        @Path("id") characterId: Int,
        @Query("label_ids") labelIds: List<Long>?,
        @Query("standing") standing: Float,
        @Query("watched") watched: Boolean?,
        @Header("Authorization") authorization: String,
        @Body contactIds: List<Int>,
    ): Response<Unit>

    @GET("/characters/{id}/online/")
    suspend fun getCharacterIdOnline(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): CharacterIdOnline

    @GET("/characters/{id}/ship/")
    suspend fun getCharacterIdShip(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): CharacterIdShip

    @GET("/characters/{id}/location/")
    suspend fun getCharacterIdLocation(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): CharacterIdLocation

    @GET("/characters/{id}/wallet/")
    suspend fun getCharactersIdWallet(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): Double

    @GET("/characters/{id}/search/")
    suspend fun getCharactersIdSearch(
        @Path("id") characterId: Int,
        @Query("categories") categories: List<String>,
        @Query("strict") strict: Boolean,
        @Query("search") search: String,
        @Header("Authorization") authorization: String,
    ): CharactersIdSearch

    @GET("/characters/{id}/clones/")
    suspend fun getCharactersIdClones(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): CharactersIdClones

    @GET("/characters/{id}/implants/")
    suspend fun getCharactersIdImplants(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): List<Int>

    @GET("/universe/stations/{id}/")
    suspend fun getUniverseStationsId(
        @Path("id") stationId: Int,
    ): UniverseStationsId

    @GET("/universe/structures/{id}/")
    suspend fun getUniverseStructuresId(
        @Path("id") structureId: Long,
        @Header("Authorization") authorization: String,
    ): UniverseStructuresId

    @GET("/universe/system_jumps/")
    suspend fun getUniverseSystemJumps(): List<UniverseSystemJumps>

    @GET("/universe/system_kills/")
    suspend fun getUniverseSystemKills(): List<UniverseSystemKills>

    @GET("/incursions/")
    suspend fun getIncursions(): List<Incursion>

    @GET("/fw/systems/")
    suspend fun getFactionWarfareSystems(): List<FactionWarfareSystem>

    @GET("/sovereignty/map/")
    suspend fun getSovereigntyMap(): List<SovereigntySystem>

    @POST("/ui/autopilot/waypoint/")
    suspend fun postUiAutopilotWaypoint(
        @Query("add_to_beginning") addToBeginning: Boolean,
        @Query("clear_other_waypoints") clearOtherWaypoints: Boolean,
        @Query("destination_id") destinationId: Long,
        @Header("Authorization") authorization: String,
    ): Response<Unit>

    @GET("/characters/{id}/assets/")
    suspend fun getCharactersIdAssets(
        @Path("id") characterId: Int,
        @Query("page") page: Int,
        @Header("Authorization") authorization: String,
    ): Response<List<CharactersIdAsset>>

    @POST("/characters/{id}/assets/names/")
    suspend fun getCharactersIdAssetsNames(
        @Path("id") characterId: Int,
        @Body assets: List<Long>,
        @Header("Authorization") authorization: String,
    ): List<CharactersIdAssetsName>

    @POST("/characters/{id}/assets/locations/")
    suspend fun getCharactersIdAssetsLocations(
        @Path("id") characterId: Int,
        @Body itemIds: List<Long>,
        @Header("Authorization") authorization: String,
    ): List<CharactersIdAssetsLocation>

    @GET("/markets/prices/")
    suspend fun getMarketsPrices(): List<MarketsPrice>

    @GET("/characters/{id}/fleet/")
    suspend fun getCharactersIdFleet(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): CharactersIdFleet

    @GET("/fleets/{id}/")
    suspend fun getFleetsId(
        @Path("id") fleetId: Long,
        @Header("Authorization") authorization: String,
    ): FleetsId

    @GET("/fleets/{id}/members/")
    suspend fun getFleetsIdMembers(
        @Path("id") fleetId: Long,
        @Header("Authorization") authorization: String,
    ): List<FleetMember>

    @GET("/characters/{id}/planets/")
    suspend fun getCharactersIdPlanets(
        @Path("id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): List<CharactersIdPlanet>

    @GET("/characters/{character_id}/planets/{planet_id}/")
    suspend fun getCharactersIdPlanetsId(
        @Path("character_id") characterId: Int,
        @Path("planet_id") planetId: Int,
        @Header("Authorization") authorization: String,
    ): CharactersIdPlanetsId

    @GET("/industry/systems/")
    suspend fun getIndustrySystems(): List<IndustrySystem>

    @GET("/corporations/{id}/projects")
    suspend fun getCorporationsIdProjects(
        @Path("id") corporationId: Int,
        @Query("before") before: String?,
        @Query("after") after: String?,
        @Query("limit") limit: Int?,
        @Query("state") state: CorporationProjectsQueryState?,
        @Header("Authorization") authorization: String,
    ): CorporationsIdProjects

    @GET("/corporations/{corporation_id}/projects/{project_id}")
    suspend fun getCorporationsIdProjectsId(
        @Path("corporation_id") corporationId: Int,
        @Path("project_id") projectId: String,
        @Header("Authorization") authorization: String,
    ): CorporationsIdProjectsId

    @GET("/corporations/{corporation_id}/projects/{project_id}/contribution/{character_id}")
    suspend fun getCorporationsIdProjectsIdContribution(
        @Path("corporation_id") corporationId: Int,
        @Path("project_id") projectId: String,
        @Path("character_id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): CorporationsIdProjectsIdContribution

    @GET("/corporations/{corporation_id}/projects/{project_id}/contributors")
    suspend fun getCorporationsIdProjectsIdContributors(
        @Path("corporation_id") corporationId: Int,
        @Path("project_id") projectId: String,
        @Query("before") before: String?,
        @Query("after") after: String?,
        @Query("limit") limit: Int?,
        @Header("Authorization") authorization: String,
    ): CorporationsIdProjectsIdContributors

    @GET("/characters/{character_id}/roles")
    suspend fun getCharactersIdRoles(
        @Path("character_id") characterId: Int,
        @Header("Authorization") authorization: String,
    ): CharactersIdRoles

    @POST("/ui/openwindow/information")
    suspend fun postUiOpenWindowInformation(
        @Query("target_id") id: Long,
        @Header("Authorization") authorization: String,
    )

    @POST("/ui/openwindow/marketdetails")
    suspend fun postUiOpenWindowMarketDetails(
        @Query("type_id") typeId: Long,
        @Header("Authorization") authorization: String,
    )

    @POST("/ui/openwindow/newmail")
    suspend fun postUiOpenWindowNewMail(
        @Body request: NewMailRequest,
        @Header("Authorization") authorization: String,
    )
}
