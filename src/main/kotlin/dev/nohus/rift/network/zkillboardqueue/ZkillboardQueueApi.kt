package dev.nohus.rift.network.zkillboardqueue

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.network.requests.Reply
import dev.nohus.rift.network.requests.RequestExecutor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Single
class ZkillboardQueueApi(
    @Named("network") json: Json,
    @Named("zkillredisq") client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://zkillredisq.stream/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
    private val service = retrofit.create(ZkillboardQueueService::class.java)

    suspend fun getKillmailRedirect(originator: Originator, queueId: String, timeToWait: Int, filter: String): Result<Reply<Unit>> {
        return executeWithHeaders { service.getKillmailRedirect(originator, queueId, timeToWait, "y", filter) }
    }
}
