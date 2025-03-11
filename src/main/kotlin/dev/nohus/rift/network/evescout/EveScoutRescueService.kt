package dev.nohus.rift.network.evescout

import retrofit2.http.GET

interface EveScoutRescueService {

    @GET("/v2/public/observations")
    suspend fun getObservations(): List<Observation>

    @GET("/v2/public/signatures")
    suspend fun getSignatures(): List<Signature>
}
