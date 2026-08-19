package dev.nohus.rift.network.zkillboard

import dev.nohus.rift.network.Result
import dev.nohus.rift.network.requests.Originator
import dev.nohus.rift.network.requests.RequestExecutor
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Path

@Single
class ZkillboardApi(
    @Named("network") json: Json,
    @Named("zkill") client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://zkillboard.com/api/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
    private val service = retrofit.create(ZkillboardService::class.java)

    suspend fun getRecentActivity(originator: Originator): Result<RecentActivity> {
        return execute { service.getRecentActivity(originator) }
    }

    suspend fun getCharacterStats(originator: Originator, characterId: Int): Result<ZkillCharacterStats> {
        return execute { service.getCharacterStats(originator, characterId) }
    }

    suspend fun postKillmail(originator: Originator, killId: String, hash: String, delay: Int): Result<PostedKillmail> {
        return execute { service.postKillmail(originator, killId, hash, delay) }
    }
}
