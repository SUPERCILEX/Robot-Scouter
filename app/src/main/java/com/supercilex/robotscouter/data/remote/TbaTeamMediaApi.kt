package com.supercilex.robotscouter.data.remote

import com.google.gson.JsonObject
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface TbaTeamMediaApi {
    @POST("image")
    fun postToImgur(
            @Header("Authorization") auth: String,
            @Query("title") title: String,
            @Body file: RequestBody): Call<JsonObject>

    @Multipart
    @POST("suggest/media/team/frc{number}/{mYear}")
    fun postToTba(
            @Path("number") number: String,
            @Path("mYear") year: Int,
            @Query("X-TBA-Auth-Key") auth: String,
            @Part("media_url") url: RequestBody): Call<JsonObject>
}
