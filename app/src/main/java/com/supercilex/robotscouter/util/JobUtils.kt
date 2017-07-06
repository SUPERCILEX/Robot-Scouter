package com.supercilex.robotscouter.util

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.support.annotation.RequiresApi
import com.firebase.jobdispatcher.Constraint
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.firebase.jobdispatcher.Job
import com.firebase.jobdispatcher.Trigger
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.util.TeamHelper

private val NUMBER = "number"
private val KEY = "key"
private val TEMPLATE_KEY = "template-key"
private val NAME = "name"
private val MEDIA = "media"
private val WEBSITE = "website"
private val CUSTOM_NAME = "custom-name"
private val CUSTOM_MEDIA = "custom-media"
private val CUSTOM_WEBSITE = "custom-website"
private val SHOULD_UPLOAD_MEDIA = "should-upload-media-to-tba"
private val MEDIA_YEAR = "media-year"
private val TIMESTAMP = "timestamp"

private fun Job.Builder.buildAndSchedule(dispatcher: FirebaseJobDispatcher) {
    val result: Int = dispatcher.schedule(build())
    if (result != FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
        throw IllegalStateException(getErrorMessage(service, result))
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun JobInfo.Builder.buildAndSchedule(context: Context, clazz: String) {
    val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    val result: Int = scheduler.schedule(build())
    if (result != JobScheduler.RESULT_SUCCESS) {
        throw IllegalStateException(getErrorMessage(clazz, result))
    }
}

fun startInternetJob14(context: Context,
                       teamHelper: TeamHelper,
                       jobId: Int,
                       clazz: Class<out com.firebase.jobdispatcher.JobService>) {
    val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context.applicationContext))

    dispatcher.newJobBuilder()
            .setService(clazz)
            .setTag(jobId.toString())
            .setTrigger(Trigger.executionWindow(0, 0))
            .setExtras(toRawBundle(teamHelper))
            .setConstraints(Constraint.ON_ANY_NETWORK)
            .buildAndSchedule(dispatcher)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun startInternetJob21(context: Context,
                       teamHelper: TeamHelper,
                       jobId: Int,
                       clazz: Class<out JobService>) {
    JobInfo.Builder(jobId, ComponentName(context.packageName, clazz.name))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setExtras(toRawPersistableBundle(teamHelper))
            .buildAndSchedule(context, clazz.name)
}

private fun getErrorMessage(clazz: String, result: Int): String =
        clazz + " failed with error code " + result

fun parseRawBundle(args: Bundle): TeamHelper = Team.Builder(args.getString(NUMBER))
        .setKey(args.getString(KEY))
        .setTemplateKey(args.getString(TEMPLATE_KEY))
        .setName(args.getString(NAME))
        .setMedia(args.getString(MEDIA))
        .setWebsite(args.getString(WEBSITE))
        .setHasCustomName(args.getBoolean(CUSTOM_NAME))
        .setHasCustomMedia(args.getBoolean(CUSTOM_MEDIA))
        .setHasCustomWebsite(args.getBoolean(CUSTOM_WEBSITE))
        .setShouldUploadMediaToTba(args.getBoolean(SHOULD_UPLOAD_MEDIA))
        .setMediaYear(args.getInt(MEDIA_YEAR))
        .setTimestamp(args.getLong(TIMESTAMP))
        .build()
        .helper

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun parseRawBundle(args: PersistableBundle): TeamHelper = Team.Builder(args.getString(NUMBER))
        .setKey(args.getString(KEY))
        .setTemplateKey(args.getString(TEMPLATE_KEY))
        .setName(args.getString(NAME))
        .setMedia(args.getString(MEDIA))
        .setWebsite(args.getString(WEBSITE))
        .setHasCustomName(args.getInt(CUSTOM_NAME) == 1)
        .setHasCustomMedia(args.getInt(CUSTOM_MEDIA) == 1)
        .setHasCustomWebsite(args.getInt(CUSTOM_WEBSITE) == 1)
        .setShouldUploadMediaToTba(args.getInt(SHOULD_UPLOAD_MEDIA) == 1)
        .setMediaYear(args.getInt(MEDIA_YEAR))
        .setTimestamp(args.getLong(TIMESTAMP))
        .build()
        .helper

private fun toRawBundle(teamHelper: TeamHelper): Bundle = Bundle().apply {
    val team: Team = teamHelper.team

    putString(NUMBER, team.number)
    putString(KEY, team.key)
    putString(TEMPLATE_KEY, team.templateKey)
    putString(NAME, team.name)
    putString(MEDIA, team.media)
    putString(WEBSITE, team.website)
    team.hasCustomName?.let { putBoolean(CUSTOM_NAME, it) }
    team.hasCustomMedia?.let { putBoolean(CUSTOM_MEDIA, it) }
    team.hasCustomWebsite?.let { putBoolean(CUSTOM_WEBSITE, it) }
    team.shouldUploadMediaToTba?.let { putBoolean(SHOULD_UPLOAD_MEDIA, it) }
    putInt(MEDIA_YEAR, team.mediaYear)
    putLong(TIMESTAMP, team.timestamp)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun toRawPersistableBundle(teamHelper: TeamHelper): PersistableBundle = PersistableBundle().apply {
    val team: Team = teamHelper.team

    putString(NUMBER, team.number)
    putString(KEY, team.key)
    putString(TEMPLATE_KEY, team.templateKey)
    putString(NAME, team.name)
    putString(MEDIA, team.media)
    putString(WEBSITE, team.website)
    team.hasCustomName?.let { putInt(CUSTOM_NAME, if (it) 1 else 0) }
    team.hasCustomMedia?.let { putInt(CUSTOM_MEDIA, if (it) 1 else 0) }
    team.hasCustomWebsite?.let { putInt(CUSTOM_WEBSITE, if (it) 1 else 0) }
    team.shouldUploadMediaToTba?.let { putInt(SHOULD_UPLOAD_MEDIA, if (it) 1 else 0) }
    putInt(MEDIA_YEAR, team.mediaYear)
    putLong(TIMESTAMP, team.timestamp)
}
