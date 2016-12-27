package com.supercilex.robotscouter.data.job;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.remote.TbaApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DownloadTeamDataJob21 extends JobService {
    public static void start(Team team, Context context) {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString("key", team.getKey());
        bundle.putString("template-key", team.getTemplateKey());
        bundle.putString("number", team.getNumber());
        bundle.putString("name", team.getName());
        bundle.putString("website", team.getWebsite());
        bundle.putString("media", team.getMedia());
        if (team.getHasCustomName() != null) {
            bundle.putInt("custom-name", team.getHasCustomName() ? 1 : 0);
        }
        if (team.getHasCustomWebsite() != null) {
            bundle.putInt("custom-website", team.getHasCustomWebsite() ? 1 : 0);
        }
        if (team.getHasCustomMedia() != null) {
            bundle.putInt("custom-media", team.getHasCustomMedia() ? 1 : 0);
        }
        bundle.putLong("timestamp", team.getTimestamp());

        JobInfo jobInfo = new JobInfo.Builder(Integer.parseInt(team.getNumber()),
                                              new ComponentName(context.getPackageName(),
                                                                DownloadTeamDataJob21.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setExtras(bundle)
                .build();

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int result = scheduler.schedule(jobInfo);
        if (result != JobScheduler.RESULT_SUCCESS) {
            FirebaseCrash.report(new RuntimeException("DownloadTeamDataJob21 failed"));
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        PersistableBundle extras = params.getExtras();
        final Team oldTeam = new Team.Builder(extras.getString("number"))
                .setKey(extras.getString("key"))
                .setTemplateKey(extras.getString("template-key"))
                .setName(extras.getString("name"))
                .setWebsite(extras.getString("website"))
                .setMedia(extras.getString("media"))
                .setHasCustomName(extras.getInt("custom-name") == 1)
                .setHasCustomWebsite(extras.getInt("custom-website") == 1)
                .setHasCustomMedia(extras.getInt("custom-media") == 1)
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
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
