package com.supercilex.robotscouter.data.client;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.Trigger;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.remote.TbaApi;
import com.supercilex.robotscouter.data.util.TeamHelper;

public class DownloadTeamDataJob14 extends JobService {
    public static void start(Context context, TeamHelper teamHelper) {
        Bundle bundle = new Bundle();
        bundle.putString("key", teamHelper.getTeam().getKey());
        bundle.putString("template-key", teamHelper.getTeam().getTemplateKey());
        bundle.putString("number", teamHelper.getTeam().getNumber());
        bundle.putString("name", teamHelper.getTeam().getName());
        bundle.putString("website", teamHelper.getTeam().getWebsite());
        bundle.putString("media", teamHelper.getTeam().getMedia());
        if (teamHelper.getTeam().getHasCustomName() != null) {
            bundle.putBoolean("custom-name", teamHelper.getTeam().getHasCustomName());
        }
        if (teamHelper.getTeam().getHasCustomWebsite() != null) {
            bundle.putBoolean("custom-website", teamHelper.getTeam().getHasCustomWebsite());
        }
        if (teamHelper.getTeam().getHasCustomMedia() != null) {
            bundle.putBoolean("custom-media", teamHelper.getTeam().getHasCustomMedia());
        }
        bundle.putLong("timestamp", teamHelper.getTeam().getTimestamp());

        FirebaseJobDispatcher dispatcher =
                new FirebaseJobDispatcher(new GooglePlayDriver(context.getApplicationContext()));

        Job job = dispatcher.newJobBuilder()
                .setService(DownloadTeamDataJob14.class)
                .setTag(teamHelper.getTeam().getNumber())
                .setTrigger(Trigger.executionWindow(0, 0))
                .setExtras(bundle)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();

        int result = dispatcher.schedule(job);
        if (result != FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
            FirebaseCrash.report(new RuntimeException("DownloadTeamDataJob14 failed with code: "
                                                              + result));
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Bundle extras = params.getExtras();
        final TeamHelper oldTeamHelper = new Team.Builder(extras.getString("number"))
                .setKey(extras.getString("key"))
                .setTemplateKey(extras.getString("template-key"))
                .setName(extras.getString("name"))
                .setWebsite(extras.getString("website"))
                .setMedia(extras.getString("media"))
                .setHasCustomName(extras.getBoolean("custom-name"))
                .setHasCustomWebsite(extras.getBoolean("custom-website"))
                .setHasCustomMedia(extras.getBoolean("custom-media"))
                .setTimestamp(extras.getLong("timestamp"))
                .build()
                .getHelper();

        TbaApi.fetch(oldTeamHelper.getTeam(), getApplicationContext())
                .addOnSuccessListener(new OnSuccessListener<Team>() {
                    @Override
                    public void onSuccess(Team newTeam) {
                        oldTeamHelper.updateTeam(newTeam);
                        jobFinished(params, false);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        jobFinished(params, true);
                    }
                });
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return true;
    }
}
