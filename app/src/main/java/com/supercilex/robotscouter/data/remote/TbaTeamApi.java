package com.supercilex.robotscouter.data.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

import static com.supercilex.robotscouter.data.remote.TbaServiceBase.APP_ID_QUERY;

public interface TbaTeamApi {
    @GET("frc{number}" + APP_ID_QUERY)
    Call<JsonObject> getInfo(@Path("number") String number);

    @GET("frc{number}/{year}/media" + APP_ID_QUERY)
    Call<JsonArray> getMedia(@Path("number") String number, @Path("year") String year);
}
