package com.supercilex.robotscouter.data.remote;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.TaskExecutor;

import java.io.IOException;
import java.util.concurrent.Callable;

import retrofit2.Response;

public final class TbaService implements Callable<Team> {
    private static final int ERROR_404 = 404;
    private Team mTeam;
    private Context mContext;
    private TbaApi mTbaApi;
    private TaskCompletionSource<Void> mInfoTask = new TaskCompletionSource<>();
    private TaskCompletionSource<Void> mMediaTask = new TaskCompletionSource<>();

    private TbaService(Team team, Context context) {
        mTeam = new Team(team);
        mContext = context;
        mTbaApi = TbaApi.RETROFIT.create(TbaApi.class);
    }

    public static Task<Team> fetch(Team team, Context context) {
        return TaskExecutor.execute(new TbaService(team, context));
    }

    @Override
    public Team call() throws Exception { // NOPMD
        getTeamInfo();
        getTeamMedia();
        Tasks.await(Tasks.whenAll(mInfoTask.getTask(), mMediaTask.getTask()));
        return mTeam;
    }

    private void getTeamInfo() throws IOException {
        Response<JsonObject> response
                = mTbaApi.getTeamInfo(mTeam.getNumber(), Constants.TOKEN).execute();

        if (!canContinue(mInfoTask, response)) return;

        JsonObject result = response.body();
        JsonElement teamNickname = result.get(Constants.TEAM_NICKNAME);
        if (teamNickname != null && !teamNickname.isJsonNull()) {
            mTeam.setName(teamNickname.getAsString());
        }
        JsonElement teamWebsite = result.get(Constants.TEAM_WEBSITE);
        if (teamWebsite != null && !teamWebsite.isJsonNull()) {
            mTeam.setWebsite(teamWebsite.getAsString());
        }

        mInfoTask.setResult(null);
    }

    private void getTeamMedia() throws IOException {
        Response<JsonArray> response
                = mTbaApi.getTeamMedia(mTeam.getNumber(), Constants.TOKEN).execute();

        if (!canContinue(mMediaTask, response)) return;

        JsonArray result = response.body();
        for (int i = 0; i < result.size(); i++) {
            JsonObject mediaObject = result.get(i).getAsJsonObject();
            String mediaType = mediaObject.get("type").getAsString();

            if (mediaType != null) {
                if (mediaType.equals("imgur")) {
                    String url = "https://i.imgur.com/"
                            + mediaObject.get("foreign_key").getAsString() + ".png";

                    setAndCacheMedia(url);
                    break;
                } else if (mediaType.equals("cdphotothread")) {
                    String url = "https://www.chiefdelphi.com/media/img/"
                            + mediaObject.get("details")
                            .getAsJsonObject()
                            .get("image_partial")
                            .getAsString();

                    setAndCacheMedia(url);
                    break;
                }
            }
        }

        mMediaTask.setResult(null);
    }

    private boolean canContinue(TaskCompletionSource<Void> task, Response response) {
        if (response.isSuccessful()) {
            return true;
        } else if (response.code() == ERROR_404) {
            task.setResult(null);
            return false;
        } else {
            throw new IllegalStateException("Error code: "
                                                    + response.code()
                                                    + "\n Error message: "
                                                    + response.errorBody().toString());
        }
    }

    private void setAndCacheMedia(final String url) {
        mTeam.setMedia(url);
        BaseHelper.runOnMainThread(mContext, new Runnable() {
            @Override
            public void run() {
                Glide.with(mContext).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).preload();
            }
        });
    }
}
