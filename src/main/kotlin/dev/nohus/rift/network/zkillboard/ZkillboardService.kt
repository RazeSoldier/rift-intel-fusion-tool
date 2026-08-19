package dev.nohus.rift.network.zkillboard

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Tag

interface ZkillboardService {

    @GET("recentactivity/")
    @EndpointTag(Endpoint.ZkillboardRecentActivity::class)
    suspend fun getRecentActivity(
        @Tag originator: Originator,
    ): RecentActivity

    @GET("stats/characterID/{id}/")
    @EndpointTag(Endpoint.ZkillboardCharacterStats::class)
    suspend fun getCharacterStats(
        @Tag originator: Originator,
        @Path("id") id: Int,
    ): ZkillCharacterStats

    @POST("killmail/add/{killId}/{hash}/{delay}/")
    @EndpointTag(Endpoint.ZkillboardPostKillmail::class)
    suspend fun postKillmail(
        @Tag originator: Originator,
        @Path("killId") killId: String,
        @Path("hash") hash: String,
        @Path("delay") delay: Int,
    ): PostedKillmail
}
