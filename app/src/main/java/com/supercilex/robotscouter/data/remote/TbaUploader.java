package com.supercilex.robotscouter.data.remote;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.gson.JsonObject;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TbaUploader extends TbaServiceBase<TbaTeamMediaApi> {
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
//        uploadToTba();
        return mTeam;
    }

    private void uploadToImgur() throws IOException {
        Response<JsonObject> response = IMGUR_RETROFIT.create(TbaTeamMediaApi.class)
                .postToImgur(mContext.getString(R.string.imgur_client_id),
                             mTeam.toString(),
                             RequestBody.create(MediaType.parse("image/*"),
                                                new File(mTeam.getMedia())))
                .execute();

        if (cannotContinue(response)) return;

        mTeam.setMedia(response.body().get("data").getAsJsonObject().get("link").getAsString());
    }

    private void uploadToTba() throws IOException {
        Response<JsonObject> response = mApi.postToTba(mTeam.getNumber()).execute();
        if (cannotContinue(response)) throw new IllegalStateException();
    }
}
