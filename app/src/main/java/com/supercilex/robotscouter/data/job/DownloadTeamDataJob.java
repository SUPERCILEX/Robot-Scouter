package com.supercilex.robotscouter.data.job;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.remote.TbaService;
import com.supercilex.robotscouter.util.BaseHelper;

public class DownloadTeamDataJob extends JobService {
    private static final String NUMBER = "number";
    private static final String KEY = "key";

    public static void start(Team team) {
        Bundle bundle = new Bundle();
        bundle.putString(NUMBER, team.getNumber());
        bundle.putString(KEY, team.getKey());

        Job job = BaseHelper.getDispatcher().newJobBuilder()
                .setService(DownloadTeamDataJob.class)
                .setTag(team.getNumber())
                .setExtras(bundle)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();

        int result = BaseHelper.getDispatcher().schedule(job);
        if (result != FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
            FirebaseCrash.report(new RuntimeException("DownloadTeamDataJob failed with code: " + result));
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        String number = params.getExtras().getString(NUMBER);
        String key = params.getExtras().getString(KEY);
        TbaService.start(new Team(number, key), getApplicationContext())
                .addOnCompleteListener(new OnCompleteListener<Team>() {
                    @Override
                    public void onComplete(@NonNull Task<Team> task) {
                        if (task.isSuccessful()) {
                            task.getResult().update();
                            jobFinished(params, false);
                        } else {
                            jobFinished(params, true);
                        }
                    }
                });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return true;
    }
}
