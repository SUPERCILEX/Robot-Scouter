package com.supercilex.robotscouter.data.client;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.firebase.jobdispatcher.Trigger;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.remote.TbaApi;
import com.supercilex.robotscouter.util.BaseHelper;

public class DownloadTeamDataJob14 extends JobService {
    public static void start(Team team) {
        Bundle bundle = new Bundle();
        bundle.putString("key", team.getKey());
        bundle.putString("template-key", team.getTemplateKey());
        bundle.putString("number", team.getNumber());
        bundle.putString("name", team.getName());
        bundle.putString("website", team.getWebsite());
        bundle.putString("media", team.getMedia());
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
                .setService(DownloadTeamDataJob14.class)
                .setTag(team.getNumber())
                .setTrigger(Trigger.executionWindow(0, 0))
                .setExtras(bundle)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();

        int result = BaseHelper.getDispatcher().schedule(job);
        if (result != FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
            FirebaseCrash.report(new RuntimeException("DownloadTeamDataJob14 failed with code: "
                                                              + result));
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Bundle extras = params.getExtras();
        final Team oldTeam = new Team.Builder(extras.getString("number"))
                .setKey(extras.getString("key"))
                .setTemplateKey(extras.getString("template-key"))
                .setName(extras.getString("name"))
                .setWebsite(extras.getString("website"))
                .setMedia(extras.getString("media"))
                .setHasCustomName(extras.getBoolean("custom-name"))
                .setHasCustomWebsite(extras.getBoolean("custom-website"))
                .setHasCustomMedia(extras.getBoolean("custom-media"))
                .setTimestamp(extras.getLong("timestamp"))
                .build();

        TbaApi.fetch(oldTeam, getApplicationContext())
                .addOnSuccessListener(new OnSuccessListener<Team>() {
                    @Override
                    public void onSuccess(Team newTeam) {
                        oldTeam.update(newTeam);
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
