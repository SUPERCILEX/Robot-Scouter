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

public final class JobHelper {
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
            throw new IllegalStateException(String.valueOf(result));
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
            throw new IllegalStateException(String.valueOf(result));
        }
    }

    public static TeamHelper parseRawBundle(Bundle args) {
        return new Team.Builder(args.getString("number"))
                .setKey(args.getString("key"))
                .setTemplateKey(args.getString("template-key"))
                .setName(args.getString("name"))
                .setMedia(args.getString("media"))
                .setWebsite(args.getString("website"))
                .setHasCustomName(args.getBoolean("custom-name"))
                .setHasCustomMedia(args.getBoolean("custom-media"))
                .setHasCustomWebsite(args.getBoolean("custom-website"))
                .setShouldUploadMediaToTba(args.getBoolean("should-upload-media-to-tba"))
                .setMediaYear(args.getInt("media-year"))
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
                .setMedia(args.getString("media"))
                .setWebsite(args.getString("website"))
                .setHasCustomName(args.getInt("custom-name") == 1)
                .setHasCustomMedia(args.getInt("custom-media") == 1)
                .setHasCustomWebsite(args.getInt("custom-website") == 1)
                .setShouldUploadMediaToTba(args.getInt("should-upload-media-to-tba") == 1)
                .setMediaYear(args.getInt("media-year"))
                .setTimestamp(args.getLong("timestamp"))
                .build()
                .getHelper();
    }

    private static Bundle toRawBundle(TeamHelper teamHelper) {
        Bundle args = new Bundle();
        Team team = teamHelper.getTeam();

        args.putString("key", team.getKey());
        args.putString("template-key", team.getTemplateKey());
        args.putString("number", team.getNumber());
        args.putString("name", team.getName());
        args.putString("media", team.getMedia());
        args.putString("website", team.getWebsite());
        if (team.getHasCustomName() != null) {
            args.putBoolean("custom-name", team.getHasCustomName());
        }
        if (team.getHasCustomMedia() != null) {
            args.putBoolean("custom-media", team.getHasCustomMedia());
        }
        if (team.getHasCustomWebsite() != null) {
            args.putBoolean("custom-website", team.getHasCustomWebsite());
        }
        if (team.getShouldUploadMediaToTba() != null) {
            args.putBoolean("should-upload-media-to-tba", team.getShouldUploadMediaToTba());
        }
        args.putInt("media-year", team.getMediaYear());
        args.putLong("timestamp", team.getTimestamp());

        return args;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static PersistableBundle toRawPersistableBundle(TeamHelper teamHelper) {
        PersistableBundle args = new PersistableBundle();
        Team team = teamHelper.getTeam();

        args.putString("key", team.getKey());
        args.putString("template-key", team.getTemplateKey());
        args.putString("number", team.getNumber());
        args.putString("name", team.getName());
        args.putString("media", team.getMedia());
        args.putString("website", team.getWebsite());
        if (team.getHasCustomName() != null) {
            args.putInt("custom-name", team.getHasCustomName() ? 1 : 0);
        }
        if (team.getHasCustomMedia() != null) {
            args.putInt("custom-media", team.getHasCustomMedia() ? 1 : 0);
        }
        if (team.getHasCustomWebsite() != null) {
            args.putInt("custom-website", team.getHasCustomWebsite() ? 1 : 0);
        }
        if (team.getShouldUploadMediaToTba() != null) {
            args.putInt("should-upload-media-to-tba", team.getShouldUploadMediaToTba() ? 1 : 0);
        }
        args.putInt("media-year", team.getMediaYear());
        args.putLong("timestamp", team.getTimestamp());

        return args;
    }
}
