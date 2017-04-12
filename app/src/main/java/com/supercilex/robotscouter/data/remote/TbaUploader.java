package com.supercilex.robotscouter.data.remote;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class TbaUploader extends TbaServiceBase<TbaTeamMediaApi> {
    private static final Retrofit IMGUR_RETROFIT = new Retrofit.Builder()
            .baseUrl("https://api.imgur.com/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static final int ERROR_400 = 400;

    private TbaUploader(Team team, Context context) {
        super(team, context, TbaTeamMediaApi.class);
    }

    public static Task<Team> upload(Team team, Context context) {
        return executeAsync(new TbaUploader(team, context));
    }

    @Override
    public Team call() throws Exception {
        uploadToImgur();
        uploadToTba();
        return mTeam;
    }

    private void uploadToImgur() throws IOException {
        Response<JsonObject> response = IMGUR_RETROFIT.create(TbaTeamMediaApi.class)
                .postToImgur(mContext.getString(R.string.imgur_client_id),
                             mTeam.toString(),
                             RequestBody.create(MediaType.parse("image/*"),
                                                new File(mTeam.getMedia())))
                .execute();

        if (cannotContinue(response)) throw new IllegalStateException();

        String link = response.body().get("data").getAsJsonObject().get("link").getAsString();
        // Oh Imgur, why don't you use https by default? 😢
        mTeam.setMedia(link.contains("https://") ? link : link.replace("http://", "https://"));
    }

    private void uploadToTba() throws IOException {
        String jsonBody = new GsonBuilder().create()
                .toJson(Collections.singletonMap("media_url", mTeam.getMedia()));

        Response<JsonObject> response =
                mApi.postToTba(mTeam.getNumber(), getYear(), getTbaApiKey(), jsonBody).execute();

        Log.d("TAG", jsonBody);
        Log.d("TAG", response.toString());
        Log.d("TAG", response.body().toString());
        if (cannotContinue(response)) return;

        if (!response.body().get("success").getAsBoolean()) throw new IllegalStateException();
    }

    @Override
    protected boolean cannotContinue(Response response) throws IllegalStateException {
        return response.code() == ERROR_400 || super.cannotContinue(response); // TODO remove if https://github.com/the-blue-alliance/the-blue-alliance/pull/1882 goes through
    }
}
