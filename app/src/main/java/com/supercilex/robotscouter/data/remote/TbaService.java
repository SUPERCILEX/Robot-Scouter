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
import com.supercilex.robotscouter.BuildConfig;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.LogFailureListener;
import com.supercilex.robotscouter.util.SimpleExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TbaService implements Callable<Team> {
    private Team mTeam;
    private Context mContext;
    private TbaApi mTbaApi;
    private TaskCompletionSource<Void> mInfoTask = new TaskCompletionSource<>();
    private TaskCompletionSource<Void> mMediaTask = new TaskCompletionSource<>();

    private TbaService(Team team, Context context) {
        mTeam = team;
        mContext = context;
        mTbaApi = TbaApi.retrofit.create(TbaApi.class);
    }

    public static Task<Team> start(Team team, Context context) {
        return Tasks.call(new SimpleExecutor(),
                          new TbaService(team, context))
                .addOnFailureListener(new LogFailureListener());
    }

    @Override
    public Team call() throws Exception {
        getTeamInfo();
        getTeamMedia();

        Task<Void> data = Tasks.whenAll(mInfoTask.getTask(), mMediaTask.getTask());
        try {
            Tasks.await(data);
        } catch (ExecutionException e) {
            throw data.getException();
        }
        return mTeam;
    }

    private void getTeamInfo() {
        mTbaApi.getTeamInfo(mTeam.getNumber(), "frc2521:Robot_Scouter:" + BuildConfig.VERSION_NAME)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
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

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        mInfoTask.setException((Exception) t);
                    }
                });
    }

    private void getTeamMedia() {
        mTbaApi.getTeamMedia(mTeam.getNumber(), "frc2521:Robot_Scouter:" + BuildConfig.VERSION_NAME)
                .enqueue(new Callback<JsonArray>() {
                    @Override
                    public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                        if (!canContinue(mMediaTask, response)) return;

                        JsonArray result = response.body();

                        for (int i = 0; i < result.size(); i++) {
                            JsonObject mediaObject = result.get(i).getAsJsonObject();
                            String mediaType = mediaObject.get("type").getAsString();

                            if (mediaType != null) {
                                if (mediaType.equals("imgur")) {
                                    String url = "https://i.imgur.com/" + mediaObject.get(
                                            "foreign_key")
                                            .getAsString() + ".png";

                                    getMedia(url);
                                    break;
                                } else if (mediaType.equals("cdphotothread")) {
                                    String url = "https://www.chiefdelphi.com/media/img/" + mediaObject
                                            .get(
                                                    "details")
                                            .getAsJsonObject()
                                            .get("image_partial")
                                            .getAsString();

                                    getMedia(url);
                                    break;
                                }
                            }
                        }

                        mMediaTask.setResult(null);
                    }

                    @Override
                    public void onFailure(Call<JsonArray> call, Throwable t) {
                        mMediaTask.setException((Exception) t);
                    }
                });
    }

    private boolean canContinue(TaskCompletionSource<Void> task, Response response) {
        if (response.isSuccessful()) {
            return true;
        } else if (response.code() == 404) {
            task.setResult(null);
            return false;
        } else {
            task.setException(new IllegalStateException("Error code: "
                                                                + response.code()
                                                                + "\n Error message: "
                                                                + response.errorBody()));
            return false;
        }
    }

    private void getMedia(String url) {
        mTeam.setMedia(url);
        Glide.with(mContext).load(url).diskCacheStrategy(DiskCacheStrategy.ALL).preload();
    }
}
