package com.supercilex.robotscouter.data.remote;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.supercilex.robotscouter.BuildConfig;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.AsyncTaskExecutor;

import java.util.concurrent.Callable;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

abstract class TbaServiceBase<T> implements Callable<Team> {
    private static final String TOKEN = "frc2521:Robot_Scouter:" + BuildConfig.VERSION_NAME;
    public static final String APP_ID_QUERY = "?X-TBA-App-Id=" + TOKEN;

    private static final Retrofit TBA_RETROFIT = new Retrofit.Builder()
            .baseUrl("https://www.thebluealliance.com/api/v2/team/")
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static final int ERROR_404 = 404;

    protected Context mContext;
    protected Team mTeam;
    protected T mApi;

    public TbaServiceBase(Team team, Context context, Class<T> clazz) {
        mTeam = new Team.Builder(team).build();
        mContext = context.getApplicationContext();
        mApi = TBA_RETROFIT.create(clazz);
    }

    protected static Task<Team> executeAsync(TbaServiceBase<?> service) {
        return AsyncTaskExecutor.execute(service);
    }

    protected boolean cannotContinue(Response response) throws IllegalStateException {
        if (response.isSuccessful()) {
            return false;
        } else if (response.code() == ERROR_404) {
            return true;
        } else {
            throw new IllegalStateException("Unknown error code: "
                                                    + response.code()
                                                    + "\n Error message: "
                                                    + response.errorBody().toString());
        }
    }
}
