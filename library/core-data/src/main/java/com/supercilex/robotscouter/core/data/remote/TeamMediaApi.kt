package com.supercilex.robotscouter.core.data.remote

import com.google.gson.JsonObject
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

internal interface TeamMediaApi {
    @POST("image")
    suspend fun postToImgurAsync(
            @Header("Authorization") auth: String,
            @Query("title") title: String,
            @Body file: RequestBody
    ): JsonObject

    @Multipart
    @POST("suggest/media/team/frc{number}/{year}")
    suspend fun postToTbaAsync(
            @Path("number") number: String,
            @Path("year") year: Int,
            @Query("X-TBA-Auth-Key") auth: String,
            @Part("media_url") url: RequestBody
    ): JsonObject

    companion object {
        val IMGUR_RETROFIT: Retrofit = Retrofit.Builder()
                .baseUrl("https://api.imgur.com/3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
}
