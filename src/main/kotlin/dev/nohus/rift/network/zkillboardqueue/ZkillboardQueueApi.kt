package dev.nohus.rift.network.zkillboardqueue
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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

@Single
class ZkillboardQueueApi(
    @Named("network") json: Json,
    @Named("zkillredisq") client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://beta.ceve-market.org/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
    private val service = retrofit.create(ZkillboardQueueService::class.java)

    suspend fun getKillmailStream(originator: Originator, queueId: String): Result<CnKillmailStreamResponse> {
        return execute { service.getKillmailStream(originator, queueId) }
    }

    suspend fun getKillmailRedirect(originator: Originator, queueId: String, timeToWait: Int, filter: String): Result<Reply<Unit>> {
        return executeWithHeaders { service.getKillmailRedirect(originator, queueId, timeToWait, filter) }
    }

    suspend fun getKillmail(originator: Originator, path: String): Result<ZkillboardQueueResponse> {
        return execute { service.getKillmail(originator, path) }
    }
}
