package dev.nohus.rift.network.evescout

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.nohus.rift.network.RequestExecutor
import dev.nohus.rift.network.Result
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.annotation.Single
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

@Single
class EveScoutRescueApi(
    json: Json,
    client: OkHttpClient,
    requestExecutor: RequestExecutor,
) : RequestExecutor by requestExecutor {

    private val contentType = "application/json".toMediaType()
    private val retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("https://api.eve-scout.com/")
        .addConverterFactory(json.asConverterFactory(contentType))
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
    private val service = retrofit.create(EveScoutRescueService::class.java)

    suspend fun getObservations(): Result<List<Observation>> {
        return execute { service.getObservations() }
    }

    suspend fun getSignatures(): Result<List<Signature>> {
        return execute { service.getSignatures() }
    }
}
