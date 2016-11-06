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
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.FirebaseUtils;

public class DownloadTeamDataJob extends JobService {
    public static void start(Team team) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.INTENT_TEAM_NUMBER, team.getNumber());
        bundle.putString(Constants.INTENT_TEAM_KEY, team.getKey());

        Job job = FirebaseUtils.getDispatcher().newJobBuilder()
                .setService(DownloadTeamDataJob.class)
                .setTag(team.getNumber())
                .setExtras(bundle)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();

        int result = FirebaseUtils.getDispatcher().schedule(job);
        if (result != FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
            FirebaseCrash.report(new RuntimeException("DownloadTeamDataJob failed with code: " + result));
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        final String number = params.getExtras().getString(Constants.INTENT_TEAM_NUMBER);
        final String key = params.getExtras().getString(Constants.INTENT_TEAM_KEY);

        TbaService.start(new Team(key, number), getApplicationContext())
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
