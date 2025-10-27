package dev.nohus.rift.network.zkillboardqueue

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface ZkillboardQueueService {

    @GET("listen.php")
    suspend fun getKillmailRedirect(
        @Query("queueID") queueId: String,
        @Query("ttw") timeToWait: Int,
        @Query("filter") filter: String,
    ): Response<Unit>

    @GET
    suspend fun getKillmail(@Url path: String): ZkillboardQueueResponse
}
