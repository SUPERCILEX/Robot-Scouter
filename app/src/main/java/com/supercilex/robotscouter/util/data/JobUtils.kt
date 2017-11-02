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
import org.jetbrains.anko.bundleOf
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
    check(result == FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
        getErrorMessage(service, result)
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun JobInfo.Builder.buildAndSchedule(clazz: String) {
    val scheduler = RobotScouter.INSTANCE.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    val result: Int = scheduler.schedule(build())
    check(result == JobScheduler.RESULT_SUCCESS) {
        getErrorMessage(clazz, result)
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
            .setExtras(team.toRawBundle())
            .setConstraints(Constraint.ON_ANY_NETWORK)
            .buildAndSchedule(dispatcher)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun startInternetJob21(team: Team, jobId: Int, clazz: Class<out JobService>) {
    JobInfo.Builder(jobId, ComponentName(RobotScouter.INSTANCE.packageName, clazz.name))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setExtras(team.toRawPersistableBundle())
            .buildAndSchedule(clazz.name)
}

private fun getErrorMessage(clazz: String, result: Int) = "$clazz failed with error code $result"

fun parseRawBundle(args: Bundle) = Team(
        args.getLong(NUMBER),
        args.getString(ID),
        args.getBundleAsMap(OWNERS),
        args.getBundleAsMap(ACTIVE_TOKENS) { Date(getLong(it)) }.toMutableMap(),
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
        args.getBundleAsMap(ACTIVE_TOKENS) { Date(getLong(it)) }.toMutableMap(),
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

private fun Team.toRawBundle() = bundleOf(
        NUMBER to number,
        ID to id,
        OWNERS to Bundle().apply {
            owners.forEach { putLong(it.key, it.value) }
        },
        ACTIVE_TOKENS to Bundle().apply {
            activeTokens.forEach { putLong(it.key, it.value.time) }
        },
        PENDING_APPROVALS to Bundle().apply {
            pendingApprovals.forEach { putString(it.key, it.value) }
        },
        TEMPLATE_ID to templateId,
        NAME to name,
        MEDIA to media,
        WEBSITE to website,
        CUSTOM_NAME to hasCustomName,
        CUSTOM_MEDIA to hasCustomMedia,
        CUSTOM_WEBSITE to hasCustomWebsite,
        SHOULD_UPLOAD_MEDIA to shouldUploadMediaToTba,
        MEDIA_YEAR to mediaYear,
        TIMESTAMP to timestamp.time)

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun Team.toRawPersistableBundle() = PersistableBundle().apply {
    putLong(NUMBER, number)
    putString(ID, id)
    putPersistableBundle(OWNERS, PersistableBundle().apply {
        owners.forEach { putLong(it.key, it.value) }
    })
    putPersistableBundle(ACTIVE_TOKENS, PersistableBundle().apply {
        activeTokens.forEach { putLong(it.key, it.value.time) }
    })
    putPersistableBundle(PENDING_APPROVALS, PersistableBundle().apply {
        pendingApprovals.forEach { putString(it.key, it.value) }
    })
    putString(TEMPLATE_ID, templateId)
    putString(NAME, name)
    putString(MEDIA, media)
    putString(WEBSITE, website)
    putBooleanCompat(CUSTOM_NAME, hasCustomName)
    putBooleanCompat(CUSTOM_MEDIA, hasCustomMedia)
    putBooleanCompat(CUSTOM_WEBSITE, hasCustomWebsite)
    putBooleanCompat(SHOULD_UPLOAD_MEDIA, shouldUploadMediaToTba)
    putInt(MEDIA_YEAR, mediaYear)
    putLong(TIMESTAMP, timestamp.time)
}
