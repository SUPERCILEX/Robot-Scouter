package com.supercilex.robotscouter.data.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

interface TbaApi {
    Retrofit RETROFIT = new Retrofit.Builder()
            .baseUrl("https://www.thebluealliance.com/api/v2/team/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    @GET("frc{number}")
    Call<JsonObject> getTeamInfo(@Path("number") String number,
                                 @Query("X-TBA-App-Id") String token);

    @GET("frc{number}/media")
    Call<JsonArray> getTeamMedia(@Path("number") String number,
                                 @Query("X-TBA-App-Id") String token);
}
