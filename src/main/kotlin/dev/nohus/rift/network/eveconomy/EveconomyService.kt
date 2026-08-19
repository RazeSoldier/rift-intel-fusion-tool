package dev.nohus.rift.network.eveconomy

import dev.nohus.rift.network.requests.Endpoint
import dev.nohus.rift.network.requests.EndpointTag
import dev.nohus.rift.network.requests.Originator
import retrofit2.http.GET
import retrofit2.http.Tag

interface EveconomyService {

    @GET("structures.json")
    @EndpointTag(Endpoint.EveconomyStructures::class)
    suspend fun getStructures(
        @Tag originator: Originator,
    ): Map<Long, String>
}
