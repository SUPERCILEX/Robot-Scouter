package com.supercilex.robotscouter.data.remote;

import android.content.Context;

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
        uploadToImgur();
        if (mTeam.getShouldUploadMediaToTba() != null) uploadToTba();
//        if (mTeam.getShouldUploadMediaToTba() != null) Log.d("TAG", "tba upload"); // uploadToTba();
        return mTeam;
    }

    private void uploadToImgur() throws IOException {
        Response<JsonObject> response = IMGUR_RETROFIT.create(TbaTeamMediaApi.class)
                .postToImgur(mContext.getString(R.string.imgur_client_id),
                             mTeam.toString(),
                             RequestBody.create(MediaType.parse("image/*"),
                                                new File(mTeam.getMedia())))
                .execute();

        if (cannotContinue(response)) throw new IllegalStateException(response.toString());

        String link = response.body().get("data").getAsJsonObject().get("link").getAsString();
        // Oh Imgur, why don't you use https by default? 😢
        link = link.startsWith("https://") ? link : link.replace("http://", "https://");
        // And what about png?
        link = link.endsWith(".png") ? link : link.replace(getFileExtension(link), ".png");

        mTeam.setMedia(link);
    }

    private void uploadToTba() throws IOException {
        Response<JsonObject> response =
                mApi.postToTba(mTeam.getNumber(),
                               getYear(),
                               getTbaApiKey(),
                               RequestBody.create(MediaType.parse("text/*"), mTeam.getMedia()))
                        .execute();

        if (cannotContinue(response)) return;

        JsonObject body = response.body();
        if (!body.get("success").getAsBoolean()
                && !body.get("message").getAsString().equals("suggestion_exists")) {
            throw new IllegalStateException();
        }
    }

    /**
     * @return Return a period plus the file extension
     */
    private String getFileExtension(String url) {
        List<String> splitUrl = Arrays.asList(url.split("\\."));
        return "." + splitUrl.get(splitUrl.size() - 1);
    }
}
