package dev.nohus.rift.network.zkillboardqueue

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.nohus.rift.network.RequestExecutor
import dev.nohus.rift.network.Result
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Single
import retrofit2.Retrofit

@Single
class ZkillboardQueueApi(
    json: Json,
    client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://zkillredisq.stream/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
    private val service = retrofit.create(ZkillboardQueueService::class.java)

    suspend fun getKillmail(queueId: String, timeToWait: Int): Result<ZkillboardQueueResponse> {
        return execute { service.getKillmail(queueId, timeToWait) }
    }
}
