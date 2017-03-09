package com.supercilex.robotscouter.data.remote;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.AsyncTaskExecutor;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.Callable;

import retrofit2.Response;

public final class TbaApi implements Callable<Team> {
    private static final String TEAM_NICKNAME = "nickname";
    private static final String TEAM_WEBSITE = "website";
    private static final String IMGUR = "imgur";
    private static final String CHIEF_DELPHI = "cdphotothread";
    private static final int ERROR_404 = 404;
    private static final int MAX_HISTORY = 2000;

    private Team mTeam;
    private Context mContext;
    private TbaService mTbaService;
    private TaskCompletionSource<Void> mInfoTask = new TaskCompletionSource<>();
    private TaskCompletionSource<Void> mMediaTask = new TaskCompletionSource<>();

    private TbaApi(Team team, Context context) {
        mTeam = new Team.Builder(team).build();
        mContext = context.getApplicationContext();
        mTbaService = TbaService.RETROFIT.create(TbaService.class);
    }

    public static Task<Team> fetch(Team team, Context context) {
        return AsyncTaskExecutor.execute(new TbaApi(team, context));
    }

    @Override
    public Team call() throws Exception {
        getTeamInfo();
        getTeamMedia(Calendar.getInstance().get(Calendar.YEAR));
        Tasks.await(Tasks.whenAll(mInfoTask.getTask(), mMediaTask.getTask()));
        return mTeam;
    }

    private void getTeamInfo() throws IOException {
        Response<JsonObject> response =
                mTbaService.getTeamInfo(mTeam.getNumber()).execute();

        if (cannotContinue(mInfoTask, response)) return;

        JsonObject result = response.body();
        JsonElement teamNickname = result.get(TEAM_NICKNAME);
        if (teamNickname != null && !teamNickname.isJsonNull()) {
            mTeam.setName(teamNickname.getAsString());
        }
        JsonElement teamWebsite = result.get(TEAM_WEBSITE);
        if (teamWebsite != null && !teamWebsite.isJsonNull()) {
            mTeam.setWebsite(teamWebsite.getAsString());
        }

        mInfoTask.setResult(null);
    }

    private void getTeamMedia(int year) throws IOException {
        Response<JsonArray> response =
                mTbaService.getTeamMedia(mTeam.getNumber(), String.valueOf(year)).execute();

        if (cannotContinue(mMediaTask, response)) return;

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

        if (mTeam.getMedia() == null && year > MAX_HISTORY) {
            getTeamMedia(year - 1);
            return;
        }

        mMediaTask.setResult(null);
    }

    private boolean cannotContinue(TaskCompletionSource<Void> task, Response response) {
        if (response.isSuccessful()) {
            return false;
        } else if (response.code() == ERROR_404) {
            task.setResult(null);
            return true;
        } else {
            throw new IllegalStateException("Error code: "
                                                    + response.code()
                                                    + "\n Error message: "
                                                    + response.errorBody().toString());
        }
    }

    private void setAndCacheMedia(final String url) {
        mTeam.setMedia(url);
        new Handler(mContext.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Glide.with(mContext)
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                        .preload();
            }
        });
    }
}
