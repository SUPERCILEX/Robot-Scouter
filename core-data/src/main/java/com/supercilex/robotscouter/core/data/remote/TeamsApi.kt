package com.supercilex.robotscouter.core.data.remote

import com.google.gson.JsonArray
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TeamsApi {
    @GET("event/{key}/teams/keys")
    fun getTeams(
            @Path("key") eventKey: String,
            @Query("X-TBA-Auth-Key") auth: String
    ): Call<JsonArray>
}
