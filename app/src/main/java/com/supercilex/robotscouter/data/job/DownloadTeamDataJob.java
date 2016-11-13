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
    public static void start(Team team) {
        // Ugh https://github.com/firebase/firebase-jobdispatcher-android/issues/51
        Bundle bundle = new Bundle();
        bundle.putString("key", team.getKey());
        bundle.putString("number", team.getNumber());
        if (team.getName() != null) bundle.putString("name", team.getName());
        if (team.getWebsite() != null) bundle.putString("website", team.getWebsite());
        if (team.getMedia() != null) bundle.putString("media", team.getMedia());
        if (team.getHasCustomName() != null) {
            bundle.putBoolean("custom-name", team.getHasCustomName());
        }
        if (team.getHasCustomWebsite() != null) {
            bundle.putBoolean("custom-website", team.getHasCustomWebsite());
        }
        if (team.getHasCustomMedia() != null) {
            bundle.putBoolean("custom-media", team.getHasCustomMedia());
        }
        bundle.putLong("timestamp", team.getTimestamp());

        Job job = BaseHelper.getDispatcher().newJobBuilder()
                .setService(DownloadTeamDataJob.class)
                .setTag(team.getNumber())
                .setExtras(/*BaseHelper.getTeamBundle(team)*/ bundle)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();

        int result = BaseHelper.getDispatcher().schedule(job);
        if (result != FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
            FirebaseCrash.report(new RuntimeException("DownloadTeamDataJob failed with code: " + result));
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
//        final Team team = BaseHelper.getTeam(params.getExtras());
        final Team team = new Team();
        team.setKey(params.getExtras().getString("key"));
        team.setNumber(params.getExtras().getString("number"));
        team.setName(params.getExtras().getString("name"));
        team.setWebsite(params.getExtras().getString("website"));
        team.setMedia(params.getExtras().getString("media"));
        if (params.getExtras().getBoolean("custom-name")) team.setHasCustomName(true);
        if (params.getExtras().getBoolean("custom-website")) team.setHasCustomWebsite(true);
        if (params.getExtras().getBoolean("custom-media")) team.setHasCustomMedia(true);
        team.setTimestamp(params.getExtras().getLong("timestamp"));

        TbaService.start(team, getApplicationContext())
                .addOnCompleteListener(new OnCompleteListener<Team>() {
                    @Override
                    public void onComplete(@NonNull Task<Team> task) {
                        if (task.isSuccessful()) {
                            team.update(task.getResult());
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
