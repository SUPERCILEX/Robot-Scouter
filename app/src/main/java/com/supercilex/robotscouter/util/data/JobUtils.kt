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
import java.util.Date

private const val NUMBER = "number"
private const val ID = "id"
private const val OWNERS = "owners"
private const val ACTIVE_TOKENS = "activeTokens"
private const val PENDING_APPROVALS = "pendingApprovals"
private const val TEMPLATE_ID = "template-id"
private const val NAME = "name"
private const val MEDIA = "media"
private const val WEBSITE = "website"
private const val CUSTOM_NAME = "custom-name"
private const val CUSTOM_MEDIA = "custom-media"
private const val CUSTOM_WEBSITE = "custom-website"
private const val SHOULD_UPLOAD_MEDIA = "should-upload-media-to-tba"
private const val MEDIA_YEAR = "media-year"
private const val TIMESTAMP = "timestamp"

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
        args.getLong(NUMBER),
        args.getString(ID),
        args.getBundleAsMap(OWNERS),
        args.getBundleAsMap<Date>(ACTIVE_TOKENS).toMutableMap(),
        args.getBundleAsMap(PENDING_APPROVALS),
        args.getString(TEMPLATE_ID),
        args.getString(NAME),
        args.getString(MEDIA),
        args.getString(WEBSITE),
        args.getBoolean(CUSTOM_NAME),
        args.getBoolean(CUSTOM_MEDIA),
        args.getBoolean(CUSTOM_WEBSITE),
        args.getBoolean(SHOULD_UPLOAD_MEDIA),
        args.getInt(MEDIA_YEAR),
        Date(args.getLong(TIMESTAMP)))

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun parseRawBundle(args: PersistableBundle) = Team(
        args.getLong(NUMBER),
        args.getString(ID),
        args.getBundleAsMap(OWNERS),
        args.getBundleAsMap<Date>(ACTIVE_TOKENS).toMutableMap(),
        args.getBundleAsMap(PENDING_APPROVALS),
        args.getString(TEMPLATE_ID),
        args.getString(NAME),
        args.getString(MEDIA),
        args.getString(WEBSITE),
        args.getBooleanCompat(CUSTOM_NAME),
        args.getBooleanCompat(CUSTOM_MEDIA),
        args.getBooleanCompat(CUSTOM_WEBSITE),
        args.getBooleanCompat(SHOULD_UPLOAD_MEDIA),
        args.getInt(MEDIA_YEAR),
        Date(args.getLong(TIMESTAMP)))

private fun toRawBundle(team: Team) = Bundle().apply {
    putLong(NUMBER, team.number)
    putString(ID, team.id)
    putBundle(OWNERS, Bundle().apply { team.owners.forEach { putLong(it.key, it.value) } })
    putBundle(ACTIVE_TOKENS, Bundle().apply {
        team.activeTokens.forEach { putLong(it.key, it.value.time) }
    })
    putBundle(PENDING_APPROVALS, Bundle().apply {
        team.pendingApprovals.forEach { putString(it.key, it.value) }
    })
    putString(TEMPLATE_ID, team.templateId)
    putString(NAME, team.name)
    putString(MEDIA, team.media)
    putString(WEBSITE, team.website)
    putBoolean(CUSTOM_NAME, team.hasCustomName)
    putBoolean(CUSTOM_MEDIA, team.hasCustomMedia)
    putBoolean(CUSTOM_WEBSITE, team.hasCustomWebsite)
    putBoolean(SHOULD_UPLOAD_MEDIA, team.shouldUploadMediaToTba)
    putInt(MEDIA_YEAR, team.mediaYear)
    putLong(TIMESTAMP, team.timestamp.time)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun toRawPersistableBundle(team: Team) = PersistableBundle().apply {
    putLong(NUMBER, team.number)
    putString(ID, team.id)
    putPersistableBundle(OWNERS, PersistableBundle().apply {
        team.owners.forEach { putLong(it.key, it.value) }
    })
    putPersistableBundle(ACTIVE_TOKENS, PersistableBundle().apply {
        team.activeTokens.forEach { putLong(it.key, it.value.time) }
    })
    putPersistableBundle(PENDING_APPROVALS, PersistableBundle().apply {
        team.pendingApprovals.forEach { putString(it.key, it.value) }
    })
    putString(TEMPLATE_ID, team.templateId)
    putString(NAME, team.name)
    putString(MEDIA, team.media)
    putString(WEBSITE, team.website)
    putBooleanCompat(CUSTOM_NAME, team.hasCustomName)
    putBooleanCompat(CUSTOM_MEDIA, team.hasCustomMedia)
    putBooleanCompat(CUSTOM_WEBSITE, team.hasCustomWebsite)
    putBooleanCompat(SHOULD_UPLOAD_MEDIA, team.shouldUploadMediaToTba)
    putInt(MEDIA_YEAR, team.mediaYear)
    putLong(TIMESTAMP, team.timestamp.time)
}
