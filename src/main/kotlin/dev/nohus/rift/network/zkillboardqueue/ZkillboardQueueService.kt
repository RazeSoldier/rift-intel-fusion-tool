package dev.nohus.rift.network.zkillboardqueue

import retrofit2.http.GET
import retrofit2.http.Query

interface ZkillboardQueueService {

    @GET("listen.php")
    suspend fun getKillmail(
        @Query("queueID") queueId: String,
        @Query("ttw") timeToWait: Int,
    ): ZkillboardQueueResponse
}
