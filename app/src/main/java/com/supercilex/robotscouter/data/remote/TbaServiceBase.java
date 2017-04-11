package com.supercilex.robotscouter.data.remote;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.AsyncTaskExecutor;

import java.util.Calendar;
import java.util.concurrent.Callable;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

abstract class TbaServiceBase<T> implements Callable<Team> {
    private static final Retrofit TBA_RETROFIT = new Retrofit.Builder()
            .baseUrl("https://www.thebluealliance.com/api/v3/")
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

    protected String getTbaApiKey() {
        return mContext.getString(R.string.tba_api_key);
    }

    protected int getYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
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
