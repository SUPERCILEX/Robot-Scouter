package com.supercilex.robotscouter.util.data

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
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team

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
private fun JobInfo.Builder.buildAndSchedule(clazz: String) {
    val scheduler = RobotScouter.INSTANCE.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    val result: Int = scheduler.schedule(build())
    if (result != JobScheduler.RESULT_SUCCESS) {
        throw IllegalStateException(getErrorMessage(clazz, result))
    }
}

fun startInternetJob14(team: Team,
                       jobId: Int,
                       clazz: Class<out com.firebase.jobdispatcher.JobService>) {
    val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(RobotScouter.INSTANCE))

    dispatcher.newJobBuilder()
            .setService(clazz)
            .setTag(jobId.toString())
            .setTrigger(Trigger.executionWindow(0, 0))
            .setExtras(toRawBundle(team))
            .setConstraints(Constraint.ON_ANY_NETWORK)
            .buildAndSchedule(dispatcher)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun startInternetJob21(team: Team, jobId: Int, clazz: Class<out JobService>) {
    JobInfo.Builder(jobId, ComponentName(RobotScouter.INSTANCE.packageName, clazz.name))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setExtras(toRawPersistableBundle(team))
            .buildAndSchedule(clazz.name)
}

private fun getErrorMessage(clazz: String, result: Int) = "$clazz failed with error code $result"

fun parseRawBundle(args: Bundle) = Team(
        args.getString(NUMBER),
        args.getString(KEY),
        args.getString(TEMPLATE_KEY),
        args.getString(NAME),
        args.getString(MEDIA),
        args.getString(WEBSITE),
        args.getBoolean(CUSTOM_NAME),
        args.getBoolean(CUSTOM_MEDIA),
        args.getBoolean(CUSTOM_WEBSITE),
        args.getBoolean(SHOULD_UPLOAD_MEDIA),
        args.getInt(MEDIA_YEAR),
        args.getLong(TIMESTAMP))

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun parseRawBundle(args: PersistableBundle) = Team(
        args.getString(NUMBER),
        args.getString(KEY),
        args.getString(TEMPLATE_KEY),
        args.getString(NAME),
        args.getString(MEDIA),
        args.getString(WEBSITE),
        args.getBooleanCompat(CUSTOM_NAME),
        args.getBooleanCompat(CUSTOM_MEDIA),
        args.getBooleanCompat(CUSTOM_WEBSITE),
        args.getBooleanCompat(SHOULD_UPLOAD_MEDIA),
        args.getInt(MEDIA_YEAR),
        args.getLong(TIMESTAMP))

private fun toRawBundle(team: Team) = Bundle().apply {
    putString(NUMBER, team.number)
    putString(KEY, team.key)
    putString(TEMPLATE_KEY, team.templateKey)
    putString(NAME, team.name)
    putString(MEDIA, team.media)
    putString(WEBSITE, team.website)
    putBoolean(CUSTOM_NAME, team.hasCustomName)
    putBoolean(CUSTOM_MEDIA, team.hasCustomMedia)
    putBoolean(CUSTOM_WEBSITE, team.hasCustomWebsite)
    putBoolean(SHOULD_UPLOAD_MEDIA, team.shouldUploadMediaToTba)
    putInt(MEDIA_YEAR, team.mediaYear)
    putLong(TIMESTAMP, team.timestamp)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun toRawPersistableBundle(team: Team) = PersistableBundle().apply {
    putString(NUMBER, team.number)
    putString(KEY, team.key)
    putString(TEMPLATE_KEY, team.templateKey)
    putString(NAME, team.name)
    putString(MEDIA, team.media)
    putString(WEBSITE, team.website)
    putBooleanCompat(CUSTOM_NAME, team.hasCustomName)
    putBooleanCompat(CUSTOM_MEDIA, team.hasCustomMedia)
    putBooleanCompat(CUSTOM_WEBSITE, team.hasCustomWebsite)
    putBooleanCompat(SHOULD_UPLOAD_MEDIA, team.shouldUploadMediaToTba)
    putInt(MEDIA_YEAR, team.mediaYear)
    putLong(TIMESTAMP, team.timestamp)
}
