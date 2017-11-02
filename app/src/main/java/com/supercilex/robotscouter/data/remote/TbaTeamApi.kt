package com.supercilex.robotscouter.data.remote

import com.google.gson.JsonArray
import com.google.gson.JsonObject

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TbaTeamApi {
    @GET("team/frc{number}")
    fun getInfo(
            @Path("number") number: String,
            @Query("X-TBA-Auth-Key") auth: String
    ): Call<JsonObject>

    @GET("team/frc{number}/media/{year}")
    fun getMedia(
            @Path("number") number: String,
            @Path("year") year: Int,
            @Query("X-TBA-Auth-Key") auth: String
    ): Call<JsonArray>
}
