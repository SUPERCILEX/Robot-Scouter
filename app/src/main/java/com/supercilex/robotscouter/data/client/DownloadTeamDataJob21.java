package com.supercilex.robotscouter.data.client;

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
import com.supercilex.robotscouter.data.util.TeamHelper;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DownloadTeamDataJob21 extends JobService {
    public static void start(Context context, TeamHelper teamHelper) {
        PersistableBundle args = new PersistableBundle();
        args.putString("key", teamHelper.getTeam().getKey());
        args.putString("template-key", teamHelper.getTeam().getTemplateKey());
        args.putString("number", teamHelper.getTeam().getNumber());
        args.putString("name", teamHelper.getTeam().getName());
        args.putString("website", teamHelper.getTeam().getWebsite());
        args.putString("media", teamHelper.getTeam().getMedia());
        if (teamHelper.getTeam().getHasCustomName() != null) {
            args.putInt("custom-name", teamHelper.getTeam().getHasCustomName() ? 1 : 0);
        }
        if (teamHelper.getTeam().getHasCustomWebsite() != null) {
            args.putInt("custom-website", teamHelper.getTeam().getHasCustomWebsite() ? 1 : 0);
        }
        if (teamHelper.getTeam().getHasCustomMedia() != null) {
            args.putInt("custom-media", teamHelper.getTeam().getHasCustomMedia() ? 1 : 0);
        }
        args.putLong("timestamp", teamHelper.getTeam().getTimestamp());

        JobInfo jobInfo = new JobInfo.Builder(Integer.parseInt(teamHelper.getTeam().getNumber()),
                                              new ComponentName(context.getPackageName(),
                                                                DownloadTeamDataJob21.class.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setExtras(args)
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
        final TeamHelper oldTeamHelper = new Team.Builder(extras.getString("number"))
                .setKey(extras.getString("key"))
                .setTemplateKey(extras.getString("template-key"))
                .setName(extras.getString("name"))
                .setWebsite(extras.getString("website"))
                .setMedia(extras.getString("media"))
                .setHasCustomName(extras.getInt("custom-name") == 1)
                .setHasCustomWebsite(extras.getInt("custom-website") == 1)
                .setHasCustomMedia(extras.getInt("custom-media") == 1)
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
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
