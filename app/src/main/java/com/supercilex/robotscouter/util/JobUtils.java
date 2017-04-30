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

public enum JobUtils {;
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
                .setExtras(toRawBundle(teamHelper))
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();

        int result = dispatcher.schedule(job);
        if (result != FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
            throw new IllegalStateException(getErrorMessage(clazz, result));
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
                .setExtras(toRawPersistableBundle(teamHelper))
                .build();

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int result = scheduler.schedule(jobInfo);
        if (result != JobScheduler.RESULT_SUCCESS) {
            throw new IllegalStateException(getErrorMessage(clazz, result));
        }
    }

    private static String getErrorMessage(Class clazz, int result) {
        return clazz.getName() + " failed with error code " + result;
    }

    public static TeamHelper parseRawBundle(Bundle args) {
        return new Team.Builder(args.getString(TeamKeys.NUMBER))
                .setKey(args.getString(TeamKeys.KEY))
                .setTemplateKey(args.getString(TeamKeys.TEMPLATE_KEY))
                .setName(args.getString(TeamKeys.NAME))
                .setMedia(args.getString(TeamKeys.MEDIA))
                .setWebsite(args.getString(TeamKeys.WEBSITE))
                .setHasCustomName(args.getBoolean(TeamKeys.CUSTOM_NAME))
                .setHasCustomMedia(args.getBoolean(TeamKeys.CUSTOM_MEDIA))
                .setHasCustomWebsite(args.getBoolean(TeamKeys.CUSTOM_WEBSITE))
                .setShouldUploadMediaToTba(args.getBoolean(TeamKeys.SHOULD_UPLOAD_MEDIA))
                .setMediaYear(args.getInt(TeamKeys.MEDIA_YEAR))
                .setTimestamp(args.getLong(TeamKeys.TIMESTAMP))
                .build()
                .getHelper();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static TeamHelper parseRawBundle(PersistableBundle args) {
        return new Team.Builder(args.getString(TeamKeys.NUMBER))
                .setKey(args.getString(TeamKeys.KEY))
                .setTemplateKey(args.getString(TeamKeys.TEMPLATE_KEY))
                .setName(args.getString(TeamKeys.NAME))
                .setMedia(args.getString(TeamKeys.MEDIA))
                .setWebsite(args.getString(TeamKeys.WEBSITE))
                .setHasCustomName(args.getInt(TeamKeys.CUSTOM_NAME) == 1)
                .setHasCustomMedia(args.getInt(TeamKeys.CUSTOM_MEDIA) == 1)
                .setHasCustomWebsite(args.getInt(TeamKeys.CUSTOM_WEBSITE) == 1)
                .setShouldUploadMediaToTba(args.getInt(TeamKeys.SHOULD_UPLOAD_MEDIA) == 1)
                .setMediaYear(args.getInt(TeamKeys.MEDIA_YEAR))
                .setTimestamp(args.getLong(TeamKeys.TIMESTAMP))
                .build()
                .getHelper();
    }

    private static Bundle toRawBundle(TeamHelper teamHelper) {
        Bundle args = new Bundle();
        Team team = teamHelper.getTeam();

        args.putString(TeamKeys.NUMBER, team.getNumber());
        args.putString(TeamKeys.KEY, team.getKey());
        args.putString(TeamKeys.TEMPLATE_KEY, team.getTemplateKey());
        args.putString(TeamKeys.NAME, team.getName());
        args.putString(TeamKeys.MEDIA, team.getMedia());
        args.putString(TeamKeys.WEBSITE, team.getWebsite());
        if (team.getHasCustomName() != null) {
            args.putBoolean(TeamKeys.CUSTOM_NAME, team.getHasCustomName());
        }
        if (team.getHasCustomMedia() != null) {
            args.putBoolean(TeamKeys.CUSTOM_MEDIA, team.getHasCustomMedia());
        }
        if (team.getHasCustomWebsite() != null) {
            args.putBoolean(TeamKeys.CUSTOM_WEBSITE, team.getHasCustomWebsite());
        }
        if (team.getShouldUploadMediaToTba() != null) {
            args.putBoolean(TeamKeys.SHOULD_UPLOAD_MEDIA, team.getShouldUploadMediaToTba());
        }
        args.putInt(TeamKeys.MEDIA_YEAR, team.getMediaYear());
        args.putLong(TeamKeys.TIMESTAMP, team.getTimestamp());

        return args;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static PersistableBundle toRawPersistableBundle(TeamHelper teamHelper) {
        PersistableBundle args = new PersistableBundle();
        Team team = teamHelper.getTeam();

        args.putString(TeamKeys.NUMBER, team.getNumber());
        args.putString(TeamKeys.KEY, team.getKey());
        args.putString(TeamKeys.TEMPLATE_KEY, team.getTemplateKey());
        args.putString(TeamKeys.NAME, team.getName());
        args.putString(TeamKeys.MEDIA, team.getMedia());
        args.putString(TeamKeys.WEBSITE, team.getWebsite());
        if (team.getHasCustomName() != null) {
            args.putInt(TeamKeys.CUSTOM_NAME, team.getHasCustomName() ? 1 : 0);
        }
        if (team.getHasCustomMedia() != null) {
            args.putInt(TeamKeys.CUSTOM_MEDIA, team.getHasCustomMedia() ? 1 : 0);
        }
        if (team.getHasCustomWebsite() != null) {
            args.putInt(TeamKeys.CUSTOM_WEBSITE, team.getHasCustomWebsite() ? 1 : 0);
        }
        if (team.getShouldUploadMediaToTba() != null) {
            args.putInt(TeamKeys.SHOULD_UPLOAD_MEDIA, team.getShouldUploadMediaToTba() ? 1 : 0);
        }
        args.putInt(TeamKeys.MEDIA_YEAR, team.getMediaYear());
        args.putLong(TeamKeys.TIMESTAMP, team.getTimestamp());

        return args;
    }

    private static final class TeamKeys {
        public static final String NUMBER = "number";
        public static final String KEY = "key";
        public static final String TEMPLATE_KEY = "template-key";
        public static final String NAME = "name";
        public static final String MEDIA = "media";
        public static final String WEBSITE = "website";
        public static final String CUSTOM_NAME = "custom-name";
        public static final String CUSTOM_MEDIA = "custom-media";
        public static final String CUSTOM_WEBSITE = "custom-website";
        public static final String SHOULD_UPLOAD_MEDIA = "should-upload-media-to-tba";
        public static final String MEDIA_YEAR = "media-year";
        public static final String TIMESTAMP = "timestamp";
    }
}
