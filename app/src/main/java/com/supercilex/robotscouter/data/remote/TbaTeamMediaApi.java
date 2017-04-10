package com.supercilex.robotscouter.data.remote;

import com.google.gson.JsonObject;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TbaTeamMediaApi {
    @POST("image")
    Call<JsonObject> postToImgur(@Header("Authorization") String auth,
                                 @Query("title") String title,
                                 @Body RequestBody file);

    Call<JsonObject> postToTba(@Path("number") String number);
}
