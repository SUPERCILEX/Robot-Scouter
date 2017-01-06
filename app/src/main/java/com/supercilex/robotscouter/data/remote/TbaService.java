package com.supercilex.robotscouter.data.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.supercilex.robotscouter.BuildConfig;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface TbaService {
    String TOKEN = "frc2521:Robot_Scouter:" + BuildConfig.VERSION_NAME;

    Retrofit RETROFIT = new Retrofit.Builder()
            .baseUrl("https://www.thebluealliance.com/api/v2/team/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    @GET("frc{number}?X-TBA-App-Id=" + TOKEN)
    Call<JsonObject> getTeamInfo(@Path("number") String number);

    @GET("frc{number}/{year}/media?X-TBA-App-Id=" + TOKEN)
    Call<JsonArray> getTeamMedia(@Path("number") String number, @Path("year") String year);
}
