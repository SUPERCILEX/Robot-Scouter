package com.supercilex.robotscouter.util;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.RequiresApi;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Trigger;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.TeamHelper;

public class JobHelper {
    private JobHelper() {
        throw new AssertionError("No instance for you!");
    }

    public static void startInternetJob14(Context context,
                                          TeamHelper teamHelper,
                                          int jobId,
                                          Class<? extends com.firebase.jobdispatcher.JobService> clazz) {
        FirebaseJobDispatcher dispatcher =
                new FirebaseJobDispatcher(new GooglePlayDriver(context.getApplicationContext()));

        Job job = dispatcher.newJobBuilder()
                .setService(clazz)
                .setTag(String.valueOf(jobId))
                .setTrigger(Trigger.executionWindow(0, 0))
                .setExtras(JobHelper.toRawBundle(teamHelper))
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();

        int result = dispatcher.schedule(job);
        if (result != FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
            throw new RuntimeException(String.valueOf(result));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void startInternetJob21(Context context,
                                          TeamHelper teamHelper,
                                          int jobId,
                                          Class<? extends JobService> clazz) {
        JobInfo jobInfo = new JobInfo.Builder(jobId,
                                              new ComponentName(context.getPackageName(),
                                                                clazz.getName()))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setExtras(JobHelper.toRawPersistableBundle(teamHelper))
                .build();

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int result = scheduler.schedule(jobInfo);
        if (result != JobScheduler.RESULT_SUCCESS) {
            throw new RuntimeException(String.valueOf(result));
        }
    }

    public static TeamHelper parseRawBundle(Bundle args) {
        return new Team.Builder(args.getString("number"))
                .setKey(args.getString("key"))
                .setTemplateKey(args.getString("template-key"))
                .setName(args.getString("name"))
                .setWebsite(args.getString("website"))
                .setMedia(args.getString("media"))
                .setHasCustomName(args.getBoolean("custom-name"))
                .setHasCustomWebsite(args.getBoolean("custom-website"))
                .setHasCustomMedia(args.getBoolean("custom-media"))
                .setTimestamp(args.getLong("timestamp"))
                .build()
                .getHelper();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static TeamHelper parseRawBundle(PersistableBundle args) {
        return new Team.Builder(args.getString("number"))
                .setKey(args.getString("key"))
                .setTemplateKey(args.getString("template-key"))
                .setName(args.getString("name"))
                .setWebsite(args.getString("website"))
                .setMedia(args.getString("media"))
                .setHasCustomName(args.getInt("custom-name") == 1)
                .setHasCustomWebsite(args.getInt("custom-website") == 1)
                .setHasCustomMedia(args.getInt("custom-media") == 1)
                .setTimestamp(args.getLong("timestamp"))
                .build()
                .getHelper();
    }

    private static Bundle toRawBundle(TeamHelper teamHelper) {
        Bundle args = new Bundle();

        args.putString("key", teamHelper.getTeam().getKey());
        args.putString("template-key", teamHelper.getTeam().getTemplateKey());
        args.putString("number", teamHelper.getTeam().getNumber());
        args.putString("name", teamHelper.getTeam().getName());
        args.putString("website", teamHelper.getTeam().getWebsite());
        args.putString("media", teamHelper.getTeam().getMedia());
        if (teamHelper.getTeam().getHasCustomName() != null) {
            args.putBoolean("custom-name", teamHelper.getTeam().getHasCustomName());
        }
        if (teamHelper.getTeam().getHasCustomWebsite() != null) {
            args.putBoolean("custom-website", teamHelper.getTeam().getHasCustomWebsite());
        }
        if (teamHelper.getTeam().getHasCustomMedia() != null) {
            args.putBoolean("custom-media", teamHelper.getTeam().getHasCustomMedia());
        }
        args.putLong("timestamp", teamHelper.getTeam().getTimestamp());

        return args;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static PersistableBundle toRawPersistableBundle(TeamHelper teamHelper) {
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

        return args;
    }
}
