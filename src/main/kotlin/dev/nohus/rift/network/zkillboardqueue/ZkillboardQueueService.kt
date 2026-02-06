package dev.nohus.rift.network.zkillboardqueue

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Tag

interface ZkillboardQueueService {

    @GET("listen.php")
    @EndpointTag(Endpoint.ZkillboardListen::class)
    suspend fun getKillmailRedirect(
        @Tag originator: Originator,
        @Query("queueID") queueId: String,
        @Query("ttw") timeToWait: Int,
        @Query("esi") esi: String,
        @Query("filter") filter: String,
    ): Response<Unit>
}
