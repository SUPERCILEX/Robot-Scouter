package com.supercilex.robotscouter.data.remote;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.gson.JsonObject;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

    private TbaUploader(Team team, Context context) {
        super(team, context, TbaTeamMediaApi.class);
    }

    public static Task<Team> upload(Team team, Context context) {
        return executeAsync(new TbaUploader(team, context));
    }

    @Override
    public Team call() throws Exception {
        try {
//            uploadToImgur();
            mTeam.setMedia("https://i.imgur.com/foo.png");
            uploadToTba();
        } catch (Throwable e) {
            Log.e("TAG", "call: ", e);
            throw e;
        }
        Log.d("TAG", "call: ");
        return mTeam;
    }

    private void uploadToImgur() throws IOException {
        Response<JsonObject> response = IMGUR_RETROFIT.create(TbaTeamMediaApi.class)
                .postToImgur(mContext.getString(R.string.imgur_client_id),
                             mTeam.toString(),
                             RequestBody.create(MediaType.parse("image/*"),
                                                new File(mTeam.getMedia())))
                .execute();

        Log.d("TAG", "uploadToImgur: " + response.body());
        if (cannotContinue(response)) throw new IllegalStateException();

        String link = response.body().get("data").getAsJsonObject().get("link").getAsString();
        // Oh Imgur, why don't you use https by default? 😢
        link = link.startsWith("https://") ? link : link.replace("http://", "https://");
        // And what about png?
        link = link.endsWith(".png") ? link : link.replace(getFileExtension(link), "png");

        mTeam.setMedia(link);
    }

    private void uploadToTba() throws IOException {
        String rawUrl = mTeam.getMedia();
        Log.d("TAG", "uploadToTba: " + rawUrl);
        String strippedUrl = rawUrl.replace("." + getFileExtension(rawUrl), "")
                .replace("https://i.", "");
        Log.d("TAG", "uploadToTba: " + strippedUrl);
        Response<JsonObject> response;
        try {
            response =
                    mApi.postToTba(mTeam.getNumber(), getYear(), getTbaApiKey(), strippedUrl)
                            .execute();
        } catch (Exception e) {
            Log.e("TAG", "uploadToTba: ", e);
            throw e;
        }

        Log.d("TAG", response.toString());
        if (cannotContinue(response)) return;
        Log.d("TAG", "Made it past cannotContinue");

        if (!response.body().get("success").getAsBoolean()) throw new IllegalStateException();
        Log.d("TAG", "Success!");
    }

    private String getFileExtension(String url) {
        List<String> splitUrl = Arrays.asList(url.split("\\."));
        return splitUrl.get(splitUrl.size() - 1);
    }
}
