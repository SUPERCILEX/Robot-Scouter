package com.supercilex.robotscouter.data.remote;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.supercilex.robotscouter.data.model.Team;

import java.io.IOException;
import java.util.Calendar;

import retrofit2.Response;

public class TbaDownloader extends TbaServiceBase<TbaTeamApi> {
    private static final String TEAM_NICKNAME = "nickname";
    private static final String TEAM_WEBSITE = "website";
    private static final String IMGUR = "imgur";
    private static final String CHIEF_DELPHI = "cdphotothread";
    private static final int MAX_HISTORY = 2000;

    private TbaDownloader(Team team, Context context) {
        super(team, context, TbaTeamApi.class);
    }

    public static Task<Team> load(Team team, Context context) {
        return executeAsync(new TbaDownloader(team, context));
    }

    @Override
    public Team call() throws Exception {
        getTeamInfo();
        getTeamMedia(Calendar.getInstance().get(Calendar.YEAR));
        return mTeam;
    }

    private void getTeamInfo() throws IOException {
        Response<JsonObject> response = mApi.getInfo(mTeam.getNumber()).execute();

        if (cannotContinue(response)) return;

        JsonObject result = response.body();
        JsonElement teamNickname = result.get(TEAM_NICKNAME);
        if (teamNickname != null && !teamNickname.isJsonNull()) {
            mTeam.setName(teamNickname.getAsString());
        }
        JsonElement teamWebsite = result.get(TEAM_WEBSITE);
        if (teamWebsite != null && !teamWebsite.isJsonNull()) {
            mTeam.setWebsite(teamWebsite.getAsString());
        }
    }

    private void getTeamMedia(int year) throws IOException {
        Response<JsonArray> response =
                mApi.getMedia(mTeam.getNumber(), String.valueOf(year)).execute();

        if (cannotContinue(response)) return;

        JsonArray result = response.body();
        for (int i = 0; i < result.size(); i++) {
            JsonObject mediaObject = result.get(i).getAsJsonObject();
            String mediaType = mediaObject.get("type").getAsString();

            if (TextUtils.equals(mediaType, IMGUR)) {
                String url = "https://i.imgur.com/" +
                        mediaObject.get("foreign_key").getAsString() +
                        ".png";

                setAndCacheMedia(url);
                break;
            } else if (TextUtils.equals(mediaType, CHIEF_DELPHI)) {
                String url = "https://www.chiefdelphi.com/media/img/" +
                        mediaObject.get("details")
                                .getAsJsonObject()
                                .get("image_partial")
                                .getAsString();

                setAndCacheMedia(url);
                break;
            }
        }

        if (mTeam.getMedia() == null && year > MAX_HISTORY) getTeamMedia(year - 1);
    }

    private void setAndCacheMedia(String url) {
        mTeam.setMedia(url);
        new Handler(mContext.getMainLooper()).post(() -> Glide.with(mContext)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .preload());
    }
}
