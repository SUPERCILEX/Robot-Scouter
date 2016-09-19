package com.supercilex.robotscouter.scout;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.supercilex.robotscouter.BuildConfig;
import com.supercilex.robotscouter.Constants;
import com.supercilex.robotscouter.model.team.Team;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

abstract class TbaService {
    private String mNumber;
    private Team mTeam;
    private Context mContext;
    private TbaApi mTbaApi;

    TbaService(String number, Team team, Context context) {
        mNumber = number;
        mTeam = team;
        mContext = context;

        getTeamInfo();
    }

    private void getTeamInfo() {
        mTbaApi = TbaApi.retrofit.create(TbaApi.class);
        Call<JsonObject> infoCall = mTbaApi.getTeamInfo(mNumber,
                                                        "frc2521:Robot_Scouter:" + BuildConfig.VERSION_NAME);
        infoCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject result = response.body();

                    JsonElement teamNickname = result.get(Constants.TEAM_NICKNAME);
                    if (teamNickname != null && !teamNickname.isJsonNull()) {
                        mTeam.setName(teamNickname.getAsString());
                    }

                    JsonElement teamWebsite = result.get(Constants.TEAM_WEBSITE);
                    if (teamWebsite != null && !teamWebsite.isJsonNull()) {
                        mTeam.setWebsite(teamWebsite.getAsString());
                    }
                } else if (response.code() == 404) {
                    onFinished(mTeam, true);
                    return;
                } else {
                    onFinished(mTeam, false);
                    return;
                }

                getTeamMedia();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                FirebaseCrash.report(t);
            }
        });
    }

    private void getTeamMedia() {
        // TODO: 09/11/2016 Make syncronized so doesn't have to wait for team info
        mTbaApi = TbaApi.retrofit.create(TbaApi.class);
        Call<JsonArray> mediaCall = mTbaApi.getTeamMedia(mNumber,
                                                         "frc2521:Robot_Scouter:" + BuildConfig.VERSION_NAME);
        mediaCall.enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if (response.isSuccessful()) {
                    JsonArray result = response.body();

                    for (int i = 0; i < result.size(); i++) {
                        JsonObject mediaObject = result.get(i).getAsJsonObject();
                        String mediaType = mediaObject.get("type").getAsString();

                        if (mediaType != null) {
                            if (mediaType.equals("imgur")) {
                                String url = "https://i.imgur.com/" + mediaObject.get("foreign_key")
                                        .getAsString() + ".png";

                                getMedia(url, mTeam);
                                break;
                            } else if (mediaType.equals("cdphotothread")) {
                                String url = "https://www.chiefdelphi.com/media/img/" + mediaObject.get(
                                        "details")
                                        .getAsJsonObject()
                                        .get("image_partial")
                                        .getAsString();

                                getMedia(url, mTeam);
                                break;
                            }
                        }
                    }
                } else {
                    onFinished(mTeam, false);
                    return;
                }

                onFinished(mTeam, true);
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                FirebaseCrash.report(t);
            }
        });
    }

    private void getMedia(String url, Team team) {
        team.setMedia(url);
        Glide.with(mContext).load(url).diskCacheStrategy(DiskCacheStrategy.ALL);
    }

    public abstract void onFinished(Team team, boolean isSuccess);

    private interface TbaApi {
        Retrofit retrofit = new Retrofit.Builder()
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
}
