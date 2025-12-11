package dev.nohus.rift.network.zkillboardqueue

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Tag
import retrofit2.http.Url

interface ZkillboardQueueService {

    @GET("api/public/kmqueue")
    @EndpointTag(Endpoint.ZkillboardListen::class)
    suspend fun getKillmailStream(
        @Tag originator: Originator,
        @Query("id") queueId: String,
    ): CnKillmailStreamResponse

    @GET("listen.php")
    @EndpointTag(Endpoint.ZkillboardListen::class)
    suspend fun getKillmailRedirect(
        @Tag originator: Originator,
        @Query("client_id") queueId: String,
        @Query("ttw") timeToWait: Int,
        @Query("filter") filter: String,
    ): Response<Unit>

    @GET
    @EndpointTag(Endpoint.Zkillboard::class)
    suspend fun getKillmail(
        @Tag originator: Originator,
        @Url path: String,
    ): ZkillboardQueueResponse
}
