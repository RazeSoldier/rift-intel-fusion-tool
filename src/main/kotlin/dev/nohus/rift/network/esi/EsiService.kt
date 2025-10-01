package dev.nohus.rift.network.esi

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
}
