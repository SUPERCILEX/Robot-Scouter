package com.supercilex.robotscouter.data.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TbaTeamApi {
    @GET("team/frc{number}")
    Call<JsonObject> getInfo(@Path("number") String number, @Query("X-TBA-Auth-Key") String auth);

    @GET("team/frc{number}/media/{year}")
    Call<JsonArray> getMedia(@Path("number") String number,
                             @Path("year") int year,
                             @Query("X-TBA-Auth-Key") String auth);
}
